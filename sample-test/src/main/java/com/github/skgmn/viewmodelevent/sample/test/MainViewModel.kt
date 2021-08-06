package com.github.skgmn.viewmodelevent.sample.test

import androidx.lifecycle.ViewModel
import com.github.skgmn.viewmodelevent.Event

class MainViewModel : ViewModel() {
    val navigateToChild = Event<Any>()

    fun navigateToChild() {
        navigateToChild.post(Unit)
    }
}