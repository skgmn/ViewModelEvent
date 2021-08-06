package com.github.skgmn.viewmodelevent

enum class DeliveryMode(internal val extraBufferCapacity: Int) {
    ALL(Int.MAX_VALUE),
    LATEST(1)
}