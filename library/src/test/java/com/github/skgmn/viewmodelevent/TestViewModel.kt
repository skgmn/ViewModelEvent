package com.github.skgmn.viewmodelevent

import androidx.lifecycle.ViewModel

class TestViewModel : ViewModel() {
    val viewModelEvent = ViewModelEvent<Any>()
}