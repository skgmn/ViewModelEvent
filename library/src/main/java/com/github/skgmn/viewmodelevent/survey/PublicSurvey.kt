package com.github.skgmn.viewmodelevent.survey

import kotlinx.coroutines.flow.Flow

class PublicSurvey<Q : Any, A : Any> internal constructor() : Survey<Q, A>(Poll()) {
    fun ask(question: Q): Flow<A> {
        return poll.ask(question)
    }
}