package com.github.skgmn.viewmodelevent

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

internal class EventHandlerBinding<T>(
        val lifecycleOwner: LifecycleOwner,
        val handler: suspend (T) -> Unit,
        private val onBind: (EventHandlerBinding<T>) -> Unit,
        private val onUnbind: () -> Unit) {

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            onBind(this@EventHandlerBinding)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            onUnbind()
            unbind()
        }
    }

    fun bind() {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }

    fun unbind() {
        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
    }
}