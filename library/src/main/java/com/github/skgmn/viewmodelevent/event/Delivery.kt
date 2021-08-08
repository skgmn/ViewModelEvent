package com.github.skgmn.viewmodelevent.event

import androidx.annotation.GuardedBy
import com.github.skgmn.viewmodelevent.DeliveryQueue
import com.github.skgmn.viewmodelevent.RetainedViewId
import java.util.*

class Delivery<T : Any> internal constructor() {
    @GuardedBy("queues")
    internal val queues = IdentityHashMap<RetainedViewId, DeliveryQueue<T>>()

    fun post(event: T) {
        val queueList = synchronized(queues) {
            queues.values.toList()
        }
        queueList.forEach { queue ->
            queue.offer(event)
        }
    }
}