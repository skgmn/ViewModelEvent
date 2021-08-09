package com.github.skgmn.viewmodelevent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal abstract class DeliveryQueue<Item, Receiver>(queueCapacity: Int) {
    protected val scope = CoroutineScope(Dispatchers.Main.immediate)
    protected val itemFlow = MutableSharedFlow<Item>(
        extraBufferCapacity = queueCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    protected val receiverFlow = MutableStateFlow<Receiver?>(null)

    abstract fun runConsumerLoop()

    fun setReceiver(receiver: Receiver?) {
        receiverFlow.value = receiver
    }

    fun compareAndSetReceiver(expect: Receiver?, receiver: Receiver?) {
        receiverFlow.compareAndSet(expect, receiver)
    }

    fun offer(item: Item) {
        itemFlow.tryEmit(item)
    }

    fun dispose() {
        scope.cancel()
    }
}