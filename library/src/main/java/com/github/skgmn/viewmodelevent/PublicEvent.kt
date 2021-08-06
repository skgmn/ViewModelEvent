package com.github.skgmn.viewmodelevent

class PublicEvent<T : Any> internal constructor() : Event<T>(Delivery()) {
    fun post(event: T) {
        delivery.post(event)
    }
}