package com.github.skgmn.viewmodelevent.survey

import androidx.annotation.GuardedBy
import com.github.skgmn.viewmodelevent.DeliveryQueue
import com.github.skgmn.viewmodelevent.RetainedViewId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.util.*

class Poll<Q : Any, A: Any> internal constructor() {
    @GuardedBy("queues")
    internal val queues = IdentityHashMap<RetainedViewId, DeliveryQueue<Questionnaire<Q, A>>>()

    fun ask(question: Q): Flow<A> {
        return flow {
            val queueList = synchronized(queues) {
                queues.values.toList()
            }
            if (queueList.isNotEmpty()) {
                val questionnaire = Questionnaire(question, queueList.size, this)
                queueList.forEach {
                    it.offer(questionnaire)
                }
                questionnaire.waitAllAnswers()
            }
        }
    }
}