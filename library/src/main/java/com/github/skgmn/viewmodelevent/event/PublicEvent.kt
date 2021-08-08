package com.github.skgmn.viewmodelevent.event

class PublicEvent<T : Any> internal constructor() : Event<T>(Delivery()) {
    fun post(event: T) {
        delivery.post(event)
    }
}