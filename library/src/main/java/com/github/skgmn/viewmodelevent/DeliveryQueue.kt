package com.github.skgmn.viewmodelevent

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

internal class DeliveryQueue<T>(private val deliveryMode: DeliveryMode) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val itemFlow = MutableSharedFlow<T>(
        extraBufferCapacity = deliveryMode.extraBufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val receiverFlow = MutableStateFlow(emptyReceiver<T>())

    fun runConsumerLoop() {
        scope.launch {
            if (deliveryMode == DeliveryMode.ALL) {
                itemFlow.collect { event ->
                    passToReceiver(event)
                }
            } else if (deliveryMode == DeliveryMode.LATEST) {
                itemFlow.collectLatest { event ->
                    passToReceiver(event)
                }
            }
        }
    }

    private suspend fun passToReceiver(event: T) {
        try {
            coroutineScope {
                receiverFlow.collectLatest { receiver ->
                    if (receiver === emptyReceiver<T>()) {
                        return@collectLatest
                    }
                    try {
                        receiver(event)
                        cancel()
                    } catch (e: CancellationException) {
                        // This receiver is cancelled.
                        //
                        // Radically, receiver should be reset to null when receiver is cancelled
                        // from the inside of it. But mere CancellationException cannot distinct the
                        // source of the cancellation, so just do not do that.
                        // For now, receiver is set to null from ViewModelEvent when receiver
                        // is cancelled because both conditions of them are same.
                        // (receiver is cancelled on onDestroy(), and just in time,
                        //  ViewModelEvent sets receiver null on onDestroy())
                        // I don't know what to do when this becomes not the case.
                    }
                }
            }
        } catch (e: CancellationException) {
            // canceled after successful event passing
        }
    }

    fun setReceiver(receiver: (suspend (T) -> Unit)?) {
        receiverFlow.value = receiver ?: emptyReceiver()
    }

    fun offer(item: T) {
        itemFlow.tryEmit(item)
    }

    fun dispose() {
        scope.cancel()
    }

    companion object {
        private val EMPTY_RECEIVER: suspend (Any?) -> Unit = {}

        private fun <T> emptyReceiver(): suspend (T) -> Unit = EMPTY_RECEIVER
    }
}