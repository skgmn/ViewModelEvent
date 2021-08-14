package com.github.skgmn.viewmodelevent.survey

import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.lifecycle.*
import com.github.skgmn.viewmodelevent.*
import com.github.skgmn.viewmodelevent.LifecycleBinder
import com.github.skgmn.viewmodelevent.RetainedViewId
import kotlinx.coroutines.CancellationException
import java.util.*
import kotlin.collections.set

open class Survey<Q, A> internal constructor(protected val poll: Poll<Q, A>) {
    @GuardedBy("binders")
    private val binders = WeakHashMap<LifecycleOwner, LifecycleBinder>()

    private val viewIdCallback = object : RetainedViewId.Callback {
        override fun onViewIdInvalid(id: RetainedViewId) {
            synchronized(poll.queues) {
                poll.queues.remove(id)?.dispose()
            }
        }
    }

    @MainThread
    internal fun replaceReplier(
        viewModelStoreOwner: ViewModelStoreOwner,
        lifecycleOwner: LifecycleOwner,
        deliveryMode: DeliveryMode,
        onRecreate: OnRecreate,
        replier: suspend (Q) -> A
    ) {
        val viewId = ViewModelProvider(viewModelStoreOwner).get(RetainedViewId::class.java)
        synchronized(poll.queues) {
            poll.queues[viewId]?.setReceiver(null)
        }
        synchronized(binders) {
            binders.remove(lifecycleOwner)?.unbind()

            val receiver: suspend (PollQueue.ReceiverState, Questionnaire<Q, A>) -> Boolean =
                { state, questionnaire ->
                    val lifecycle = lifecycleOwner.lifecycle
                    try {
                        lifecycle.whenStarted {
                            if (state.trySetCancellable(false)) {
                                questionnaire.answer(replier(questionnaire.question))
                            }
                            true
                        }
                    } catch (e: CancellationException) {
                        val cancelledByDestroy = e !is PollQueue.CancelledByNextItemException &&
                                lifecycle.currentState == Lifecycle.State.DESTROYED
                        if (cancelledByDestroy && onRecreate == OnRecreate.RERUN) {
                            false
                        } else {
                            throw e
                        }
                    }
                }
            val binder = LifecycleBinder(onReady = {
                val queue = synchronized(poll.queues) {
                    poll.queues[viewId]
                        ?: PollQueue<Q, A>(deliveryMode).also {
                            poll.queues[viewId] = it
                            it.runConsumerLoop()
                            viewId.addCallback(viewIdCallback)
                        }
                }
                queue.setReceiver(receiver)
            }, onUnbind = {
                synchronized(poll.queues) {
                    poll.queues[viewId]?.compareAndSetReceiver(receiver, null)
                }
                synchronized(binders) {
                    binders -= lifecycleOwner
                }
            })
            binders[lifecycleOwner] = binder
            binder.bindTo(lifecycleOwner)
        }
    }
}