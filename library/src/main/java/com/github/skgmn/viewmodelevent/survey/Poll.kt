package com.github.skgmn.viewmodelevent.survey

import androidx.annotation.GuardedBy
import com.github.skgmn.viewmodelevent.DeliveryQueue
import com.github.skgmn.viewmodelevent.RetainedViewId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.util.*

class Poll<Q, A> internal constructor() {
    @GuardedBy("queues")
    internal val queues = IdentityHashMap<RetainedViewId, DeliveryQueue<Questionnaire<Q, A>>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun ask(question: Q): Flow<A> {
        return channelFlow {
            val queueList = synchronized(queues) {
                queues.values.toList()
            }
            if (queueList.isNotEmpty()) {
                val questionnaire = Questionnaire(question, queueList.size, this)
                queueList.forEach {
                    it.offer(questionnaire)
                }
                awaitClose()
            }
        }
    }
}