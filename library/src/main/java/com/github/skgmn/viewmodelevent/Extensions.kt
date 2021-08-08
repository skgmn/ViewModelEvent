package com.github.skgmn.viewmodelevent

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.github.skgmn.viewmodelevent.event.Delivery
import com.github.skgmn.viewmodelevent.event.Event
import com.github.skgmn.viewmodelevent.event.PublicEvent
import com.github.skgmn.viewmodelevent.survey.Poll
import com.github.skgmn.viewmodelevent.survey.PublicSurvey
import com.github.skgmn.viewmodelevent.survey.Survey

fun <T : Any> ViewModel.publicEvent(): PublicEvent<T> {
    return PublicEvent()
}

fun <T : Any> ViewModel.delivery(): Delivery<T> {
    return Delivery()
}

fun <T : Any> ViewModel.event(delivery: Delivery<T>): Event<T> {
    return Event(delivery)
}

fun <Q : Any, A : Any> ViewModel.publicSurvey(): PublicSurvey<Q, A> {
    return PublicSurvey()
}

fun <Q : Any, A : Any> ViewModel.poll(): Poll<Q, A> {
    return Poll()
}

fun <Q : Any, A : Any> ViewModel.survey(poll: Poll<Q, A>): Survey<Q, A> {
    return Survey(poll)
}

@MainThread
fun <T : Any> ComponentActivity.handle(
    event: Event<T>,
    deliveryMode: DeliveryMode = DeliveryMode.LATEST,
    handler: (T) -> Unit
) {
    event.replaceHandler(this, this, deliveryMode, handler)
}

@MainThread
fun <Q : Any, A : Any> ComponentActivity.answer(
    survey: Survey<Q, A>,
    deliveryMode: DeliveryMode = DeliveryMode.LATEST,
    replier: suspend (Q) -> A
) {
    survey.replaceReplier(this, this, deliveryMode, replier)
}

@MainThread
fun <T : Any> Fragment.handle(
    event: Event<T>,
    backpressure: DeliveryMode = DeliveryMode.LATEST,
    handler: (T) -> Unit
) {
    event.replaceHandler(this, this, backpressure, handler)
}

@MainThread
fun <Q : Any, A : Any> Fragment.answer(
    survey: Survey<Q, A>,
    backpressure: DeliveryMode = DeliveryMode.LATEST,
    replier: suspend (Q) -> A
) {
    survey.replaceReplier(this, this, backpressure, replier)
}