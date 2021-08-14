package com.github.skgmn.viewmodelevent

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

internal class LifecycleBinder(
        private val onReady: () -> Unit,
        private val onUnbind: () -> Unit) {

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            onReady()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            unbind()
        }
    }

    private var lifecycleOwner: LifecycleOwner? = null
    private var unbound = false

    @MainThread
    fun bindTo(lifecycleOwner: LifecycleOwner) {
        if (!unbound && this.lifecycleOwner == null) {
            this.lifecycleOwner = lifecycleOwner
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        } else {
            throw IllegalStateException()
        }
    }

    @MainThread
    fun unbind() {
        if (unbound) {
            return
        }
        unbound = true
        lifecycleOwner?.let {
            it.lifecycle.removeObserver(lifecycleObserver)
            lifecycleOwner = null
        }
        onUnbind()
    }
}