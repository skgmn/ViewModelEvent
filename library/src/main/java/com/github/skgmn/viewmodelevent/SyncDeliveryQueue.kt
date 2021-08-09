package com.github.skgmn.viewmodelevent

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

internal class SyncDeliveryQueue<T>(
    private val deliveryMode: DeliveryMode
) : DeliveryQueue<T, suspend (T) -> Unit>(deliveryMode.extraBufferCapacity) {
    override fun runConsumerLoop() {
        scope.launch {
            when (deliveryMode) {
                DeliveryMode.ALL -> {
                    itemFlow.collect { item ->
                        passToReceiver(item)
                    }
                }
                DeliveryMode.LATEST -> {
                    itemFlow.collectLatest { item ->
                        passToReceiver(item)
                    }
                }
            }
        }
    }

    private suspend fun passToReceiver(item: T) {
        try {
            coroutineScope {
                receiverFlow.collectLatest { receiver ->
                    if (receiver == null) {
                        return@collectLatest
                    }
                    try {
                        receiver(item)
                        cancel()
                    } catch (e: CancellationException) {
                        // receiver is cancelled
                    }
                }
            }
        } catch (e: CancellationException) {
            // canceled after successful event passing
        }
    }
}