package com.github.skgmn.viewmodelevent.survey

import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.lifecycle.*
import com.github.skgmn.viewmodelevent.*
import com.github.skgmn.viewmodelevent.AsyncDeliveryQueue
import com.github.skgmn.viewmodelevent.LifecycleBinder
import com.github.skgmn.viewmodelevent.RetainedViewId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
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
        replier: suspend (Q) -> A
    ) {
        val viewId = ViewModelProvider(viewModelStoreOwner).get(RetainedViewId::class.java)
        synchronized(poll.queues) {
            poll.queues[viewId]?.setReceiver(null)
        }
        synchronized(binders) {
            binders.remove(lifecycleOwner)?.unbind()

            val receiver: suspend (AsyncDeliveryQueue.ReceiverState, Questionnaire<Q, A>) -> Unit =
                { state, questionnaire ->
                    val lifecycle = lifecycleOwner.lifecycle
                    try {
                        lifecycle.whenStarted {
                            if (!state.trySetCancellable(false)) {
                                return@whenStarted
                            }
                            questionnaire.answer(replier(questionnaire.question))
                        }
                    } catch (e: Throwable) {
                        val cancelledByLifecycleDestroy =
                            e is CancellationException &&
                                    e !is AsyncDeliveryQueue.LatestCancellationException &&
                                    lifecycle.currentState == Lifecycle.State.DESTROYED
                        if (cancelledByLifecycleDestroy) {
                            throw e
                        } else {
                            questionnaire.error(e)
                        }
                    }
                }
            val binder = LifecycleBinder(onReady = {
                val queue = synchronized(poll.queues) {
                    poll.queues[viewId]
                        ?: AsyncDeliveryQueue<Questionnaire<Q, A>>(deliveryMode).also {
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