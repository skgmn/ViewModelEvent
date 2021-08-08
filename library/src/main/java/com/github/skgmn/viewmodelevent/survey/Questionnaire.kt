package com.github.skgmn.viewmodelevent.survey

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
internal class Questionnaire<Q : Any, A : Any>(
    val question: Q,
    replierCount: Int,
    private val emitter: ProducerScope<A>
) {
    private val replierCount = AtomicInteger(replierCount)

    suspend fun answer(answer: A) {
        emitter.send(answer)
        if (replierCount.decrementAndGet() == 0) {
            emitter.close()
        }
    }
}