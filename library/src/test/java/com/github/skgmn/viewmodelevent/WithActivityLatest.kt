package com.github.skgmn.viewmodelevent

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WithActivityLatest {
    @get:Rule
    val activityScenarioRule = activityScenarioRule<TestActivity>()

    @Test
    fun immediateWhenStarted() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            assertEquals(0, activity.eventResults.size)
            activity.viewModel.normalEvent.post(1234)
            activity.viewModel.normalEvent.post(5678)
            activity.viewModel.normalEvent.post(9012)
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
            activity.viewModel.normalEvent.post(1234)
            activity.viewModel.normalEvent.post(5678)
            activity.viewModel.normalEvent.post(9012)
            activity.viewModel.normalEvent.post(3456)
            assertEquals(0, activity.eventResults.size)
            scenario.moveToState(Lifecycle.State.STARTED)
            assertEquals(1, activity.eventResults.size)
            assertEquals(3456, activity.eventResults[0])
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
                override fun onStop(owner: LifecycleOwner) {
                    activity.viewModel.normalEvent.post(1234)
                    activity.viewModel.normalEvent.post(5678)
                    activity.viewModel.normalEvent.post(9012)
                    activity.viewModel.normalEvent.post(3456)
                    assertEquals(0, activity.eventResults.size)
                    activity.lifecycle.removeObserver(this)
                }
            })
        }
        scenario.recreate()
        scenario.onActivity { activity ->
            assertEquals(1, activity.eventResults.size)
            assertEquals(3456, activity.eventResults[0])
        }
    }

    @Test
    fun noOpAfterDestroy() {
        val scenario = activityScenarioRule.scenario
        lateinit var activity: TestActivity
        // save activity instance in advance because it can't be done after destroyed
        scenario.onActivity { activity = it }
        scenario.moveToState(Lifecycle.State.DESTROYED)
        activity.viewModel.normalEvent.post(1234)
        assertEquals(0, activity.eventResults.size)
    }

    @Test
    fun recentHandlerReplacesPreviousOne() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            activity.viewModel.handledManyTimesEvent.post(1234)
            activity.viewModel.handledManyTimesEvent.post(5678)
            activity.viewModel.handledManyTimesEvent.post(9012)
            assertEquals(0, activity.eventResults.size)
            assertEquals(3, activity.eventResults2.size)
            assertEquals(1234, activity.eventResults2[0])
            assertEquals(5678, activity.eventResults2[1])
            assertEquals(9012, activity.eventResults2[2])
        }
    }

    @Test
    fun ignoreEventsBeforeFirstHandling() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            activity.viewModel.ignoredBeforeFirstHandlingEvent.post(1234)
            activity.viewModel.ignoredBeforeFirstHandlingEvent.post(5678)
            activity.viewModel.ignoredBeforeFirstHandlingEvent.post(9012)
            assertEquals(0, activity.eventResults.size)
        }
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            assertEquals(0, activity.eventResults.size)
            activity.viewModel.ignoredBeforeFirstHandlingEvent.post("foo")
            activity.viewModel.ignoredBeforeFirstHandlingEvent.post("bar")
            activity.viewModel.ignoredBeforeFirstHandlingEvent.post("baz")
            assertEquals(3, activity.eventResults.size)
            assertEquals("foo", activity.eventResults[0])
            assertEquals("bar", activity.eventResults[1])
            assertEquals("baz", activity.eventResults[2])
        }
    }

    class TestActivity : AppCompatActivity() {
        val viewModel: TestViewModel by viewModels()

        val eventResults = mutableListOf<Any>()
        val eventResults2 = mutableListOf<Any>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            viewModel.run {
                handle(normalEvent, DeliveryMode.LATEST) {
                    eventResults += it
                }
                handle(handledManyTimesEvent, DeliveryMode.LATEST) {
                    eventResults += it
                }
            }
        }

        override fun onStart() {
            super.onStart()
            viewModel.run {
                handle(handledManyTimesEvent, DeliveryMode.LATEST) {
                }
            }
        }

        override fun onResume() {
            super.onResume()
            viewModel.run {
                handle(handledManyTimesEvent, DeliveryMode.LATEST) {
                    eventResults2 += it
                }
            }
        }

        override fun onPause() {
            viewModel.run {
                handle(ignoredBeforeFirstHandlingEvent, DeliveryMode.LATEST) {
                    eventResults += it
                }
            }
            super.onPause()
        }
    }

    class TestViewModel : ViewModel() {
        val normalEvent = publicEvent<Any>()
        val handledManyTimesEvent = publicEvent<Any>()
        val ignoredBeforeFirstHandlingEvent = publicEvent<Any>()
    }
}
