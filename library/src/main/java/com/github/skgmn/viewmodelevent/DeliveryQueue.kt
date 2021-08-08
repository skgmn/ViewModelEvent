package com.github.skgmn.viewmodelevent

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

internal class DeliveryQueue<T>(
    private val async: Boolean,
    private val deliveryMode: DeliveryMode
) {
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
                    startDelivery(this, event)
                }
            } else if (deliveryMode == DeliveryMode.LATEST) {
                itemFlow.collectLatest { event ->
                    startDelivery(this, event)
                }
            }
        }
    }

    private suspend fun startDelivery(scope: CoroutineScope, item: T) {
        if (async) {
            scope.launch {
                passToReceiver(item)
            }
        } else {
            passToReceiver(item)
        }
    }

    private suspend fun passToReceiver(item: T) {
        try {
            coroutineScope {
                receiverFlow.collectLatest { receiver ->
                    if (receiver === emptyReceiver<T>()) {
                        return@collectLatest
                    }
                    try {
                        receiver(item)
                        cancel()
                    } catch (e: CancellationException) {
                        // receiver is cancelled
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

    fun compareAndSetReceiver(expect: (suspend (T) -> Unit)?, receiver: (suspend (T) -> Unit)?) {
        receiverFlow.compareAndSet(expect ?: emptyReceiver<T>(), receiver ?: emptyReceiver<T>())
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