package com.github.skgmn.viewmodelevent

import androidx.annotation.GuardedBy
import java.util.*

class Delivery<T : Any> internal constructor() {
    @GuardedBy("queues")
    internal val queues = IdentityHashMap<RetainedViewId, EventHandlerQueue<T>>()

    fun post(event: T) {
        val queueList = synchronized(queues) {
            queues.values.toList()
        }
        queueList.forEach { queue ->
            queue.offer(event)
        }
    }
}