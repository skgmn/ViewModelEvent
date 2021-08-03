package com.github.skgmn.viewmodelevent

import androidx.activity.ComponentActivity
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.util.*

class ViewModelEvent<T : Any> {
    private val lock get() = bindings

    @GuardedBy("lock")
    private val bindings = IdentityHashMap<RetainedViewId, EventHandlerBinding<T>>()

    @GuardedBy("lock")
    private val activatedBindings = mutableSetOf<RetainedViewId>()

    @GuardedBy("lock")
    private val channels = IdentityHashMap<RetainedViewId, Channel<T>>()

    private val viewIdContainer = object : RetainedViewIdContainer {
        override fun onViewIdCleared(id: RetainedViewId) {
            synchronized(lock) {
                bindings.remove(id)?.unbind()
                channels.remove(id)?.close()
                activatedBindings -= id
            }
        }
    }

    fun dispatchEvent(event: T) {
        val capturedBindings = synchronized(lock) {
            activatedBindings.mapNotNull { viewId ->
                bindings[viewId]?.let {
                    viewId to it
                }
            }
        }
        capturedBindings.forEach { (viewId, binding) ->
            dispatchEventTo(viewId, binding, event)
        }
    }

    private fun dispatchEventTo(
            viewId: RetainedViewId,
            binding: EventHandlerBinding<T>,
            event: T) {

        var eventHandled = false
        val job = binding.lifecycleOwner.lifecycleScope.launchWhenStarted {
            eventHandled = true
            binding.handler(event)
        }
        job.invokeOnCompletion {
            if (!eventHandled) {
                val channel = synchronized(lock) {
                    channels[viewId] ?: Channel<T>(Channel.UNLIMITED).also {
                        channels[viewId] = it
                    }
                }
                try {
                    channel.offer(event)
                } catch (e: ClosedSendChannelException) {
                    // ignore it
                }
            }
        }
    }

    private fun consumePendingEvents(
            viewId: RetainedViewId,
            binding: EventHandlerBinding<T>) {

        val channel = synchronized(lock) {
            channels[viewId]
        } ?: return

        try {
            while (true) {
                dispatchEventTo(viewId, binding, channel.poll() ?: break)
            }
        } catch (e: ClosedReceiveChannelException) {
            // ignore it
        }

        synchronized(lock) {
            tryRemoveViewIdContainer(viewId)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @GuardedBy("lock")
    private fun tryRemoveViewIdContainer(viewId: RetainedViewId) {
        val channel = channels[viewId]
        if (viewId !in bindings &&
                viewId !in activatedBindings &&
                (channel?.isEmpty != false || channel.isClosedForReceive)) {
            viewId.containers -= viewIdContainer
            channel?.close()
            channels -= viewId
        }
    }

    @MainThread
    internal fun replaceHandler(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            handler: suspend (T) -> Unit) {

        val viewId = ViewModelProvider(viewModelStoreOwner).get(RetainedViewId::class.java)
        viewId.containers += viewIdContainer

        synchronized(lock) {
            bindings.remove(viewId)?.unbind()

            val newBinding = EventHandlerBinding(lifecycleOwner, handler,
                    onBind = { binding ->
                        consumePendingEvents(viewId, binding)
                        synchronized(lock) {
                            activatedBindings += viewId
                        }
                    },
                    onUnbind = {
                        synchronized(lock) {
                            activatedBindings -= viewId
                            bindings -= viewId
                            tryRemoveViewIdContainer(viewId)
                        }
                    })
            bindings[viewId] = newBinding
            newBinding.bind()
        }
    }
}

@MainThread
fun <T : Any> ComponentActivity.handleEvent(
        event: ViewModelEvent<T>,
        handler: suspend (T) -> Unit) {

    event.replaceHandler(this, this, handler)
}

@MainThread
fun <T : Any> Fragment.handleEvent(event: ViewModelEvent<T>, handler: suspend (T) -> Unit) {
    event.replaceHandler(this, this, handler)
}
