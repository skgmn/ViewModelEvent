package com.github.skgmn.viewmodelevent

import androidx.annotation.MainThread
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

internal class EventHandlerQueue<T> {
    private val emptyReceiver: suspend (T) -> Unit = { }
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val channel = Channel<T>(Channel.UNLIMITED)
    private val receiverFlow = MutableStateFlow(emptyReceiver)

    fun runConsumerLoop() {
        scope.launch(Dispatchers.Main.immediate) {
            try {
                while (true) {
                    val event = channel.receive()
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
            } catch (e: ClosedReceiveChannelException) {
                // queue disposed
            }
        }
    }

    @MainThread
    fun setReceiver(receiver: (suspend (T) -> Unit)?) {
        receiverFlow.value = receiver ?: emptyReceiver
    }

    fun offer(event: T) {
        channel.offer(event)
    }

    fun dispose() {
        scope.cancel()
        channel.close()
    }
}