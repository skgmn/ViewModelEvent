package com.github.skgmn.viewmodelevent

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MultipleScreenHandleEvent {
    @get:Rule
    val activityScenarioRule = activityScenarioRule<TestActivity>()

    @Test
    fun multipleHandling() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            activity.viewModel.normalEvent.post(1234)
            activity.viewModel.normalEvent.post(5678)
            activity.viewModel.normalEvent.post(9012)

            assertEquals(3, activity.fragment1.eventResults.size)
            assertEquals(1234, activity.fragment1.eventResults[0])
            assertEquals(5678, activity.fragment1.eventResults[1])
            assertEquals(9012, activity.fragment1.eventResults[2])

            assertEquals(3, activity.fragment2.eventResults.size)
            assertEquals(1234, activity.fragment2.eventResults[0])
            assertEquals(5678, activity.fragment2.eventResults[1])
            assertEquals(9012, activity.fragment2.eventResults[2])
        }
    }

    @Test
    fun differentLifecycle() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            activity.supportFragmentManager.beginTransaction()
                .detach(activity.fragment2)
                .commitNow()

            activity.viewModel.normalEvent.post(1234)
            activity.viewModel.normalEvent.post(5678)
            activity.viewModel.normalEvent.post(9012)

            assertEquals(3, activity.fragment1.eventResults.size)
            assertEquals(1234, activity.fragment1.eventResults[0])
            assertEquals(5678, activity.fragment1.eventResults[1])
            assertEquals(9012, activity.fragment1.eventResults[2])

            assertEquals(0, activity.fragment2.eventResults.size)

            activity.supportFragmentManager.beginTransaction()
                .attach(activity.fragment2)
                .commitNow()

            assertEquals(3, activity.fragment2.eventResults.size)
            assertEquals(1234, activity.fragment2.eventResults[0])
            assertEquals(5678, activity.fragment2.eventResults[1])
            assertEquals(9012, activity.fragment2.eventResults[2])
        }
    }

    class TestActivity : AppCompatActivity() {
        val viewModel: TestViewModel by viewModels()

        val fragment1 = TestFragment()
        val fragment2 = TestFragment()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            supportFragmentManager.beginTransaction()
                .add(fragment1, null)
                .add(fragment2, null)
                .commitNow()
        }
    }

    class TestFragment : Fragment() {
        val viewModel: TestViewModel by activityViewModels()

        val eventResults = mutableListOf<Any>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            handle(viewModel.normalEvent, DeliveryMode.ALL) {
                eventResults += it
            }
        }
    }

    class TestViewModel : ViewModel() {
        val normalEvent = publicEvent<Any>()
    }
}