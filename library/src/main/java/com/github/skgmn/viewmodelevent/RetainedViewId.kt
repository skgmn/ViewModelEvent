package com.github.skgmn.viewmodelevent

import androidx.lifecycle.ViewModel
import java.lang.ref.WeakReference
import java.util.*

// This is not really a ViewModel.
// We just need an instance to be used as a key which corresponds to ViewModel's lifecycle.
internal class RetainedViewId : ViewModel() {
    val containers: MutableSet<RetainedViewIdContainer> =
            Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))

    override fun onCleared() {
        super.onCleared()
        containers.toList().forEach {
            it.onViewIdCleared(this)
        }
    }
}