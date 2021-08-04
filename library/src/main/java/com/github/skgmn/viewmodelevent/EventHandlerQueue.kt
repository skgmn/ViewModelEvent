package com.github.skgmn.viewmodelevent

import androidx.annotation.MainThread
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
                while (true) {
                    val receiver = receiverFlow.filter { it !== emptyReceiver }.first()
                    try {
                        receiver(event)
                    } catch (e: CancellationException) {
                        // this receiver is cancelled
                        receiverFlow.value = emptyReceiver
                        continue
                    }
                    break
                }
            }
        }
    }

    @MainThread
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