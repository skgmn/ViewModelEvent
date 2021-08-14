package com.github.skgmn.viewmodelevent.survey

import androidx.annotation.GuardedBy
import com.github.skgmn.viewmodelevent.RetainedViewId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import java.util.*

class Poll<Q, A> internal constructor() {
    @GuardedBy("queues")
    internal val queues = IdentityHashMap<RetainedViewId, PollQueue<Q, A>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun ask(question: Q): Flow<A> {
        return channelFlow {
            val queueList = synchronized(queues) {
                queues.values.toList()
            }
            if (queueList.isNotEmpty()) {
                val questionnaire = Questionnaire<Q, A>(question, queueList.size, this)
                queueList.forEach {
                    it.offer(questionnaire)
                }
                awaitClose()
            }
        }
            .map {
                if (it is Questionnaire.Error) {
                    throw it.cause
                } else {
                    @Suppress("UNCHECKED_CAST")
                    it as A
                }
            }
    }
}