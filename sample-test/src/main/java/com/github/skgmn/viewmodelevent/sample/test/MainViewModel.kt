package com.github.skgmn.viewmodelevent.sample.test

import com.github.skgmn.viewmodelevent.ViewModel

class MainViewModel : ViewModel() {
    val navigateToChildEvent = event<Any>()

    fun navigateToChild() {
        navigateToChildEvent.post(Unit)
    }
}