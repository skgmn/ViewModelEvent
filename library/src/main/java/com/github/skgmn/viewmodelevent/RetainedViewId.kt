package com.github.skgmn.viewmodelevent

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel

// This is not really a ViewModel.
// We just need an instance to be used as a key which corresponds to ViewModel's lifecycle.
internal class RetainedViewId : ViewModel() {
    private var cleared = false
    private val callbacks = mutableSetOf<Callback>()

    @MainThread
    fun addCallback(callback: Callback) {
        if (cleared) {
            callback.onViewIdInvalid(this)
            return
        }
        callbacks += callback
    }

    override fun onCleared() {
        super.onCleared()
        cleared = true
        callbacks.forEach {
            it.onViewIdInvalid(this)
        }
        callbacks.clear()
    }

    interface Callback {
        fun onViewIdInvalid(id: RetainedViewId)
    }
}