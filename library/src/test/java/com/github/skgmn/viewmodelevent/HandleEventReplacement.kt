package com.github.skgmn.viewmodelevent

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HandleEventReplacement {
    @get:Rule
    val activityScenarioRule = activityScenarioRule<TestActivity>()

    @Test
    fun recentHandlerReplacesPreviousOne() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            activity.viewModel.viewModelEvent.dispatchEvent(1234)
            activity.viewModel.viewModelEvent.dispatchEvent(5678)
            assertEquals(0, activity.eventResults1.size)
            assertEquals(2, activity.eventResults2.size)
            assertEquals(1234, activity.eventResults2[0])
            assertEquals(5678, activity.eventResults2[1])
        }
    }

    class TestActivity : AppCompatActivity() {
        val viewModel: TestViewModel by viewModels()

        val eventResults1 = mutableListOf<Any>()
        val eventResults2 = mutableListOf<Any>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            handleEvent(viewModel.viewModelEvent) {
                eventResults1 += it
            }
        }

        override fun onStart() {
            super.onStart()
            handleEvent(viewModel.viewModelEvent) {
                eventResults2 += it
            }
        }
    }

    class TestViewModel : ViewModel() {
        val viewModelEvent = ViewModelEvent<Any>()
    }
}