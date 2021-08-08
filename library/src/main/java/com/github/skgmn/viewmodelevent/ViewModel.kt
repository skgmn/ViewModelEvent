package com.github.skgmn.viewmodelevent

import androidx.lifecycle.ViewModel
import com.github.skgmn.viewmodelevent.event.Delivery
import com.github.skgmn.viewmodelevent.event.Event
import com.github.skgmn.viewmodelevent.survey.Poll
import com.github.skgmn.viewmodelevent.survey.Survey
import java.util.*

open class ViewModel : ViewModel() {
    private val deliveries by lazy(LazyThreadSafetyMode.NONE) {
        IdentityHashMap<Event<*>, Delivery<*>>()
    }
    private val polls by lazy(LazyThreadSafetyMode.NONE) {
        IdentityHashMap<Survey<*, *>, Poll<*, *>>()
    }

    protected fun <T> event(): Event<T> {
        val delivery = Delivery<T>()
        val event = Event(delivery)
        deliveries[event] = delivery
        return event
    }

    protected fun <Q, A> survey(): Survey<Q, A> {
        val poll = Poll<Q, A>()
        val survey = Survey(poll)
        polls[survey] = poll
        return survey
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T> Event<T>.post(event: T) {
        (deliveries[this] as? Delivery<T>)?.post(event)
            ?: throw RuntimeException("Cannot access to other ViewModel's event")
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <Q, A> Survey<Q, A>.ask(question: Q) {
        (polls[this] as? Poll<Q, A>)?.ask(question)
            ?: throw RuntimeException("Cannot access to other ViewModel's survey")
    }
}