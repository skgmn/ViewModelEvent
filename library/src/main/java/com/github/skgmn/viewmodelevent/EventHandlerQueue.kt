package com.github.skgmn.viewmodelevent

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class EventHandlerQueue<T> {
    private val emptyReceiver: suspend (T) -> Unit = { }
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val eventFlow = MutableSharedFlow<T>(
            extraBufferCapacity = Int.MAX_VALUE
    )
    private val receiverFlow = MutableStateFlow(emptyReceiver)

    fun runConsumerLoop() {
        scope.launch {
            eventFlow.collect { event ->
                try {
                    coroutineScope {
                        receiverFlow.collectLatest { receiver ->
                            if (receiver === emptyReceiver) {
                                return@collectLatest
                            }
                            try {
                                receiver(event)
                                cancel()
                            } catch (e: CancellationException) {
                                // this receiver is cancelled
                                receiverFlow.value = emptyReceiver
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    // canceled after successful event passing
                }
            }
        }
    }

    fun setReceiver(receiver: (suspend (T) -> Unit)?) {
        receiverFlow.value = receiver ?: emptyReceiver
    }

    fun offer(event: T) {
        eventFlow.tryEmit(event)
    }

    fun dispose() {
        scope.cancel()
    }
}