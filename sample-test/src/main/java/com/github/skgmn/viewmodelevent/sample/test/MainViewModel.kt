package com.github.skgmn.viewmodelevent.sample.test

import androidx.lifecycle.viewModelScope
import com.github.skgmn.viewmodelevent.ViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    val askUserToSelect = survey<String, Boolean>()
    val feedback = event<Boolean>()

    fun buttonAction() {
        viewModelScope.launch {
            val yesNo = askUserToSelect.ask("Select Yes or No").first()
            feedback.post(yesNo)
        }
    }
}