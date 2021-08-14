package com.github.skgmn.viewmodelevent.survey

import kotlinx.coroutines.flow.Flow

class PublicSurvey<Q, A> internal constructor() : Survey<Q, A>(Poll()) {
    fun ask(question: Q): Flow<A> {
        return poll.ask(question)
    }
}