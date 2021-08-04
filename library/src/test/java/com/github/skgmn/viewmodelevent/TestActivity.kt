package com.github.skgmn.viewmodelevent

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class TestActivity : AppCompatActivity() {
    val viewModel: TestViewModel by viewModels()

    val eventResults = mutableListOf<Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleEvent(viewModel.viewModelEvent) {
            eventResults += it
        }
    }
}