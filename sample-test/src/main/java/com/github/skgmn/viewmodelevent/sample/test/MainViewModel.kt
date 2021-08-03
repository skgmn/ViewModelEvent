package com.github.skgmn.viewmodelevent.sample.test

import androidx.lifecycle.ViewModel
import com.github.skgmn.viewmodelevent.ViewModelEvent

class MainViewModel : ViewModel() {
    val navigateToChild = ViewModelEvent<Any>()

    fun navigateToChild() {
        navigateToChild.dispatchEvent(Unit)
    }
}