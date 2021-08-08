package com.github.skgmn.viewmodelevent.survey

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.yield

internal class Questionnaire<Q : Any, A : Any>(
    val question: Q,
    replierCount: Int,
    private val collector: FlowCollector<A>
) {
    private val replierCount = MutableStateFlow(replierCount)

    suspend fun answer(answer: A) {
        collector.emit(answer)
        while (true) {
            val oldCount = replierCount.value
            if (replierCount.compareAndSet(oldCount, oldCount - 1)) {
                break
            }
            yield()
        }
    }

    suspend fun waitAllAnswers() {
        replierCount.dropWhile { it != 0 }.collect()
    }
}