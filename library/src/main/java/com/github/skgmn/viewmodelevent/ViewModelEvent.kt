package com.github.skgmn.viewmodelevent

import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.core.app.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.util.*

class ViewModelEvent<T : Any> {
    @GuardedBy("eventHandlerStates")
    private val eventHandlerStates = WeakHashMap<LifecycleOwner, EventHandlerState<T>>()

    fun dispatchEvent(event: T) {
        val capturedStates = synchronized(eventHandlerStates) {
            eventHandlerStates.values.toList()
        }
        capturedStates.forEach {
            it.dispatchEvent(event)
        }
    }

    @MainThread
    internal fun replaceHandler(lifecycleOwner: LifecycleOwner, handler: suspend (T) -> Unit) {
        val state = synchronized(eventHandlerStates) {
            eventHandlerStates.remove(lifecycleOwner)
        }?.also {
            it.lifecycleOwner.lifecycle.removeObserver(it.lifecycleObserver)
        } ?: EventHandlerState(lifecycleOwner)

        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                state.consumeEvents()
                synchronized(eventHandlerStates) {
                    eventHandlerStates[owner] = state
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                synchronized(eventHandlerStates) {
                    eventHandlerStates -= owner
                }
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        }

        state.handler = handler
        state.lifecycleObserver = observer
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    private class EventHandlerState<T>(val lifecycleOwner: LifecycleOwner) {
        lateinit var handler: suspend (T) -> Unit
        lateinit var lifecycleObserver: LifecycleObserver

        private val channelLazy = lazy {
            Channel<T>(Channel.UNLIMITED, BufferOverflow.DROP_OLDEST)
        }
        private val channel by channelLazy

        fun dispatchEvent(event: T) {
            var eventHandled = false
            val job = lifecycleOwner.lifecycleScope.launchWhenStarted {
                eventHandled = true
                handler(event)
            }
            job.invokeOnCompletion {
                if (!eventHandled) {
                    try {
                        channel.offer(event)
                    } catch (e: ClosedSendChannelException) {
                        // This state has already been disposed
                    }
                }
            }
        }

        fun consumeEvents() {
            if (!channelLazy.isInitialized()) {
                return
            }
            while (true) {
                dispatchEvent(channel.poll() ?: break)
            }
        }
    }
}

@MainThread
fun <T : Any> ComponentActivity.handleEvent(
        event: ViewModelEvent<T>,
        handler: suspend (T) -> Unit) {

    event.replaceHandler(this, handler)
}

@MainThread
fun <T : Any> Fragment.handleEvent(event: ViewModelEvent<T>, handler: suspend (T) -> Unit) {
    event.replaceHandler(this, handler)
}
