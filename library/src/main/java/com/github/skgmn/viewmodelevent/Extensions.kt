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

fun <T> ViewModel.publicEvent(): PublicEvent<T> {
    return PublicEvent()
}

fun <T> ViewModel.delivery(): Delivery<T> {
    return Delivery()
}

fun <T> ViewModel.event(delivery: Delivery<T>): Event<T> {
    return Event(delivery)
}

fun <Q, A> ViewModel.publicSurvey(): PublicSurvey<Q, A> {
    return PublicSurvey()
}

fun <Q, A> ViewModel.poll(): Poll<Q, A> {
    return Poll()
}

fun <Q, A> ViewModel.survey(poll: Poll<Q, A>): Survey<Q, A> {
    return Survey(poll)
}

@MainThread
fun <T> ComponentActivity.handle(
    event: Event<T>,
    deliveryMode: DeliveryMode = DeliveryMode.LATEST,
    handler: (T) -> Unit
) {
    event.replaceHandler(this, this, deliveryMode, handler)
}

@MainThread
fun <Q, A> ComponentActivity.answer(
    survey: Survey<Q, A>,
    deliveryMode: DeliveryMode = DeliveryMode.LATEST,
    replier: suspend (Q) -> A
) {
    survey.replaceReplier(this, this, deliveryMode, replier)
}

@MainThread
fun <T> Fragment.handle(
    event: Event<T>,
    deliveryMode: DeliveryMode = DeliveryMode.LATEST,
    handler: (T) -> Unit
) {
    event.replaceHandler(this, this, deliveryMode, handler)
}

@MainThread
fun <Q, A> Fragment.answer(
    survey: Survey<Q, A>,
    deliveryMode: DeliveryMode = DeliveryMode.LATEST,
    replier: suspend (Q) -> A
) {
    survey.replaceReplier(this, this, deliveryMode, replier)
}