package com.github.skgmn.viewmodelevent.survey

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
internal class Questionnaire<Q, A>(
    val question: Q,
    replierCount: Int,
    private val emitter: ProducerScope<Any?>
) {
    private val replierCount = AtomicInteger(replierCount)

    suspend fun answer(answer: A) {
        emitter.send(answer)
        tryClose()
    }

    suspend fun error(e: Throwable) {
        emitter.send(Error(e))
        tryClose()
    }

    private fun tryClose() {
        if (replierCount.decrementAndGet() == 0) {
            emitter.close()
        }
    }

    class Error(val cause: Throwable)
}