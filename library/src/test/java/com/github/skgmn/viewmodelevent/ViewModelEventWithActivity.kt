package com.github.skgmn.viewmodelevent

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewModelEventWithActivity {
    @get:Rule
    val activityScenarioRule = activityScenarioRule<TestActivity>()

    @Test
    fun immediateWhenStarted() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            assertEquals(0, activity.eventResults.size)
            activity.viewModel.viewModelEvent.dispatchEvent(1234)
            activity.viewModel.viewModelEvent.dispatchEvent(5678)
            activity.viewModel.viewModelEvent.dispatchEvent(9012)
            assertEquals(3, activity.eventResults.size)
            assertEquals(1234, activity.eventResults[0])
            assertEquals(5678, activity.eventResults[1])
            assertEquals(9012, activity.eventResults[2])
        }
    }

    @Test
    fun delayedOnStopStart() {
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity { activity ->
            assertEquals(0, activity.eventResults.size)
            activity.viewModel.viewModelEvent.dispatchEvent(Unit)
            assertEquals(0, activity.eventResults.size)
        }
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            assertEquals(1, activity.eventResults.size)
            assertEquals(Unit, activity.eventResults[0])
        }
    }

    @Test
    fun delayedOnRecreate() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            // use LifecycleObserver instead of moveToState() because
            // recreate() forces activity to be onResume state before actual recreation
            // which makes us hard to test `dispatching event after onStop` scenario.
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    activity.viewModel.viewModelEvent.dispatchEvent(1234)
                    activity.viewModel.viewModelEvent.dispatchEvent(5678)
                    activity.viewModel.viewModelEvent.dispatchEvent(9012)
                    assertEquals(0, activity.eventResults.size)
                    activity.lifecycle.removeObserver(this)
                }
            })
        }
        scenario.recreate()
        scenario.onActivity { activity ->
            println("activity lifecycle after recreate: ${activity.lifecycle.currentState}")
            assertEquals(3, activity.eventResults.size)
            assertEquals(1234, activity.eventResults[0])
            assertEquals(5678, activity.eventResults[1])
            assertEquals(9012, activity.eventResults[2])
        }
    }

    @Test
    fun noOpAfterDestroy() {
        val scenario = activityScenarioRule.scenario
        lateinit var activity: TestActivity
        // save activity instance in advance because it can't be done after destroyed
        scenario.onActivity { activity = it }
        scenario.moveToState(Lifecycle.State.DESTROYED)
        activity.viewModel.viewModelEvent.dispatchEvent(1234)
        assertEquals(0, activity.eventResults.size)
    }
}