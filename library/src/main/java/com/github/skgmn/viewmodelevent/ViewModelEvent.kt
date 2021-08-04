package com.github.skgmn.viewmodelevent

import androidx.activity.ComponentActivity
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.withContext
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

    fun dispatchEvent(event: T) {
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
        handler: (T) -> Unit
    ) {

        val viewId = ViewModelProvider(viewModelStoreOwner).get(RetainedViewId::class.java)
        synchronized(queues) {
            queues[viewId]?.setReceiver(null)
        }
        synchronized(bindings) {
            bindings.remove(lifecycleOwner)?.unbind()

            val binding = EventHandlerBinding(onReady = {
                val queue = synchronized(queues) {
                    queues[viewId] ?: EventHandlerQueue<T>().also {
                        queues[viewId] = it
                        it.runConsumerLoop()
                        viewId.addContainer(viewIdContainer)
                    }
                }
                queue.setReceiver { event ->
                    withContext(lifecycleOwner.lifecycleScope.coroutineContext) {
                        lifecycleOwner.lifecycle.whenStarted {
                            handler(event)
                        }
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
fun <T : Any> ComponentActivity.handleEvent(event: ViewModelEvent<T>, handler: (T) -> Unit) {
    event.replaceHandler(this, this, handler)
}

@MainThread
fun <T : Any> Fragment.handleEvent(event: ViewModelEvent<T>, handler: (T) -> Unit) {
    event.replaceHandler(this, this, handler)
}
