package com.github.skgmn.rapidviewmodel.sample.test

import androidx.lifecycle.ViewModel
import com.github.skgmn.rapidviewmodel.ViewModelEvent

class MainViewModel : ViewModel() {
    val navigateToChild = ViewModelEvent<Any>()

    fun navigateToChild() {
        navigateToChild.dispatchEvent(Unit)
    }
}