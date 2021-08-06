package com.github.skgmn.viewmodelevent

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel

fun <T : Any> ViewModel.publicEvent(): PublicEvent<T> {
    return PublicEvent()
}

fun <T : Any> ViewModel.delivery(): Delivery<T> {
    return Delivery()
}

fun <T : Any> ViewModel.event(delivery: Delivery<T>): Event<T> {
    return Event(delivery)
}

@MainThread
fun <T : Any> ComponentActivity.handle(
    event: Event<T>,
    backpressure: EventBackpressure = EventBackpressure.LATEST,
    handler: (T) -> Unit
) {
    event.replaceHandler(this, this, backpressure, handler)
}

@MainThread
fun <T : Any> Fragment.handle(
    event: Event<T>,
    backpressure: EventBackpressure = EventBackpressure.LATEST,
    handler: (T) -> Unit
) {
    event.replaceHandler(this, this, backpressure, handler)
}