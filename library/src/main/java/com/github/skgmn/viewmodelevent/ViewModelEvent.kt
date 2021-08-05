package com.github.skgmn.viewmodelevent

import androidx.activity.ComponentActivity
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.whenStarted
import java.util.*
import kotlin.collections.set

class ViewModelEvent<T : Any> {
    @GuardedBy("bindings")
    private val bindings = WeakHashMap<LifecycleOwner, EventHandlerBinding>()

    @GuardedBy("queues")
    private val queues = IdentityHashMap<RetainedViewId, EventHandlerQueue<T>>()

    private val viewIdContainer = object : RetainedViewIdContainer {
        override fun onViewIdCleared(id: RetainedViewId) {
            synchronized(queues) {
                queues.remove(id)?.dispose()
            }
        }
    }

    fun post(event: T) {
        val queueList = synchronized(queues) {
            queues.values.toList()
        }
        queueList.forEach { queue ->
            queue.offer(event)
        }
    }

    @MainThread
    internal fun replaceHandler(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            backpressure: EventBackpressure,
            handler: (T) -> Unit) {

        val viewId = ViewModelProvider(viewModelStoreOwner).get(RetainedViewId::class.java)
        synchronized(queues) {
            queues[viewId]?.setReceiver(null)
        }
        synchronized(bindings) {
            bindings.remove(lifecycleOwner)?.unbind()

            val binding = EventHandlerBinding(onReady = {
                val queue = synchronized(queues) {
                    queues[viewId] ?: EventHandlerQueue<T>(backpressure).also {
                        queues[viewId] = it
                        it.runConsumerLoop()
                        viewId.addContainer(viewIdContainer)
                    }
                }
                queue.setReceiver { event ->
                    lifecycleOwner.lifecycle.whenStarted {
                        handler(event)
                    }
                }
            }, onUnbind = {
                synchronized(queues) {
                    queues[viewId]?.setReceiver(null)
                }
                synchronized(bindings) {
                    bindings -= lifecycleOwner
                }
            })
            bindings[lifecycleOwner] = binding
            binding.bindTo(lifecycleOwner)
        }
    }
}

@MainThread
fun <T : Any> ComponentActivity.handle(
        event: ViewModelEvent<T>,
        backpressure: EventBackpressure = EventBackpressure.LATEST,
        handler: (T) -> Unit) {

    event.replaceHandler(this, this, backpressure, handler)
}

@MainThread
fun <T : Any> Fragment.handle(
        event: ViewModelEvent<T>,
        backpressure: EventBackpressure = EventBackpressure.LATEST,
        handler: (T) -> Unit) {

    event.replaceHandler(this, this, backpressure, handler)
}