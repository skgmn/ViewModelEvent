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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AllEvents {
    @get:Rule
    val activityScenarioRule = activityScenarioRule<TestActivity>()

    @Test
    fun immediateWhenStarted() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        val activity = scenario.getActivity()

        assertEquals(0, activity.eventResults.size)
        activity.viewModel.normalEvent.post(1234)
        activity.viewModel.normalEvent.post(5678)
        activity.viewModel.normalEvent.post(9012)
        assertEquals(3, activity.eventResults.size)
        assertEquals(1234, activity.eventResults[0])
        assertEquals(5678, activity.eventResults[1])
        assertEquals(9012, activity.eventResults[2])
    }

    @Test
    fun delayedOnStopStart() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        val activity = scenario.getActivity()

        scenario.moveToState(Lifecycle.State.CREATED)

        assertEquals(0, activity.eventResults.size)
        activity.viewModel.normalEvent.post(1234)
        activity.viewModel.normalEvent.post(5678)
        activity.viewModel.normalEvent.post(9012)
        assertEquals(0, activity.eventResults.size)

        scenario.moveToState(Lifecycle.State.STARTED)

        assertEquals(3, activity.eventResults.size)
        assertEquals(1234, activity.eventResults[0])
        assertEquals(5678, activity.eventResults[1])
        assertEquals(9012, activity.eventResults[2])
    }

    @Test
    fun delayedOnRecreate() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        var activity = scenario.getActivity()

        // use LifecycleObserver instead of moveToState() because
        // recreate() forces activity to be onResume state before actual recreation
        // which makes us hard to test `dispatching event after onStop` scenario.
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                activity.viewModel.normalEvent.post(1234)
                activity.viewModel.normalEvent.post(5678)
                activity.viewModel.normalEvent.post(9012)
                assertEquals(0, activity.eventResults.size)
                activity.lifecycle.removeObserver(this)
            }
        })

        scenario.recreate()
        activity = scenario.getActivity()

        assertEquals(3, activity.eventResults.size)
        assertEquals(1234, activity.eventResults[0])
        assertEquals(5678, activity.eventResults[1])
        assertEquals(9012, activity.eventResults[2])
    }

    @Test
    fun noOpAfterDestroy() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        val activity = scenario.getActivity()

        scenario.moveToState(Lifecycle.State.DESTROYED)
        activity.viewModel.normalEvent.post(1234)
        assertEquals(0, activity.eventResults.size)
    }

    @Test
    fun recentHandlerReplacesPreviousOne() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        val activity = scenario.getActivity()

        activity.viewModel.handledManyTimesEvent.post(1234)
        activity.viewModel.handledManyTimesEvent.post(5678)
        assertEquals(0, activity.eventResults.size)
        assertEquals(2, activity.eventResults2.size)
        assertEquals(1234, activity.eventResults2[0])
        assertEquals(5678, activity.eventResults2[1])
    }

    @Test
    fun ignoreEventsBeforeFirstHandling() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        val activity = scenario.getActivity()

        activity.viewModel.ignoredBeforeFirstHandlingEvent.post(1234)
        activity.viewModel.ignoredBeforeFirstHandlingEvent.post(5678)
        assertEquals(0, activity.eventResults.size)

        scenario.moveToState(Lifecycle.State.STARTED)

        assertEquals(0, activity.eventResults.size)
        activity.viewModel.ignoredBeforeFirstHandlingEvent.post("foo")
        activity.viewModel.ignoredBeforeFirstHandlingEvent.post("bar")
        assertEquals(2, activity.eventResults.size)
        assertEquals("foo", activity.eventResults[0])
        assertEquals("bar", activity.eventResults[1])
    }

    class TestActivity : AppCompatActivity() {
        val viewModel: TestViewModel by viewModels()

        val eventResults = mutableListOf<Any>()
        val eventResults2 = mutableListOf<Any>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            viewModel.run {
                handle(normalEvent, DeliveryMode.ALL) {
                    eventResults += it
                }
                handle(handledManyTimesEvent, DeliveryMode.ALL) {
                    eventResults += it
                }
            }
        }

        override fun onStart() {
            super.onStart()
            viewModel.run {
                handle(handledManyTimesEvent, DeliveryMode.ALL) {
                }
            }
        }

        override fun onResume() {
            super.onResume()
            viewModel.run {
                handle(handledManyTimesEvent, DeliveryMode.ALL) {
                    eventResults2 += it
                }
            }
        }

        override fun onPause() {
            viewModel.run {
                handle(ignoredBeforeFirstHandlingEvent, DeliveryMode.ALL) {
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
