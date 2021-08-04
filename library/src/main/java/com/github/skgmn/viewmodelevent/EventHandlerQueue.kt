package com.github.skgmn.viewmodelevent

import androidx.annotation.MainThread
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException

internal class EventHandlerQueue<T> {
    private val emptyReceiver: suspend (T) -> Unit = { }
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val channel = Channel<T>(Channel.UNLIMITED)
    private val receiverChannel = Channel<suspend (T) -> Unit>(
        1, BufferOverflow.DROP_OLDEST
    )

    fun runConsumerLoop() {
        var savedReceiver: (suspend (T) -> Unit)? = null
        fun pollReceiver(): (suspend (T) -> Unit)? {
            val receiver = receiverChannel.poll()
            return if (receiver == null) {
                savedReceiver
            } else {
                receiver.takeIf { it !== emptyReceiver }.also {
                    savedReceiver = it
                }
            }
        }

        scope.launch(Dispatchers.Main.immediate) {
            try {
                while (true) {
                    val event = channel.receive()
                    while (true) {
                        val receiver = pollReceiver()
                            ?: receiverChannel.receive().takeIf { it !== emptyReceiver }.also {
                                savedReceiver = it
                            }
                            ?: continue
                        try {
                            receiver(event)
                        } catch (e: CancellationException) {
                            // this receiver is cancelled
                            savedReceiver = null
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
        receiverChannel.offer(receiver ?: emptyReceiver)
    }

    fun offer(event: T) {
        channel.offer(event)
    }

    fun dispose() {
        scope.cancel()
        receiverChannel.close()
        channel.close()
    }
}