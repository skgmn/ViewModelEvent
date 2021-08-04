package com.github.skgmn.viewmodelevent

import androidx.activity.ComponentActivity
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.set
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
                    lifecycleOwner.awaitStateStarted()
                    handler(event)
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

    private suspend fun LifecycleOwner.awaitStateStarted() {
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            throw CancellationException()
        }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }
        suspendCancellableCoroutine<Any> { cont ->
            val observer = object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
                        lifecycle.removeObserver(this)
                        cont.resumeWithException(CancellationException())
                    } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        lifecycle.removeObserver(this)
                        cont.resume(Unit)
                    }
                }
            }
            lifecycle.addObserver(observer)
            cont.invokeOnCancellation {
                GlobalScope.launch(Dispatchers.Main.immediate) {
                    lifecycle.removeObserver(observer)
                }
            }
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
