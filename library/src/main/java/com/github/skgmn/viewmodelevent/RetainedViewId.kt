package com.github.skgmn.viewmodelevent

import androidx.annotation.GuardedBy
import androidx.lifecycle.ViewModel
import java.util.*

// This is not really a ViewModel.
// We just need an instance to be used as a key which corresponds to ViewModel's lifecycle.
internal class RetainedViewId : ViewModel() {
    private var cleared = false

    @GuardedBy("containers")
    private val containers: MutableSet<RetainedViewIdContainer> =
            Collections.newSetFromMap(WeakHashMap())

    fun addContainer(container: RetainedViewIdContainer) {
        if (cleared) {
            container.onViewIdCleared(this)
            return
        }
        synchronized(containers) {
            containers += container
        }
    }

    fun removeContainer(container: RetainedViewIdContainer) {
        synchronized(containers) {
            containers -= container
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleared = true
        val capturedContainers = synchronized(containers) {
            containers.toList().also {
                containers.clear()
            }
        }
        capturedContainers.forEach {
            it.onViewIdCleared(this)
        }
    }
}