package com.github.skgmn.viewmodelevent

import kotlinx.coroutines.channels.BufferOverflow

enum class EventBackpressure(
        val extraBufferCapacity: Int,
        val onBufferOverflow: BufferOverflow) {

    BUFFER(Int.MAX_VALUE, BufferOverflow.SUSPEND),
    LATEST(1, BufferOverflow.DROP_OLDEST)
}