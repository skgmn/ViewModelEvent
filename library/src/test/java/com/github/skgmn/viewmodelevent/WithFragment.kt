package com.github.skgmn.viewmodelevent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
class WithFragment {
    @get:Rule
    val activityScenarioRule = activityScenarioRule<TestActivity>()

    @Test
    fun immediateWhenStarted() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            val fragment = activity.fragment
            assertEquals(0, fragment.eventResults.size)
            fragment.viewModel.normalEvent.post(1234)
            fragment.viewModel.normalEvent.post(5678)
            fragment.viewModel.normalEvent.post(9012)
            assertEquals(3, fragment.eventResults.size)
            assertEquals(1234, fragment.eventResults[0])
            assertEquals(5678, fragment.eventResults[1])
            assertEquals(9012, fragment.eventResults[2])
        }
    }

    @Test
    fun delayedOnStopStart() {
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity { activity ->
            val fragment = activity.fragment
            assertEquals(0, fragment.eventResults.size)
            fragment.viewModel.normalEvent.post(Unit)
            assertEquals(0, fragment.eventResults.size)
        }
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            val fragment = activity.fragment
            assertEquals(1, fragment.eventResults.size)
            assertEquals(Unit, fragment.eventResults[0])
        }
    }

    @Test
    fun delayedOnRecreate() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            val fragment = activity.fragment
            // use LifecycleObserver instead of moveToState() because
            // recreate() forces activity to be onResume state before actual recreation
            // which makes us hard to test `dispatching event after onStop` scenario.
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    fragment.viewModel.normalEvent.post(1234)
                    fragment.viewModel.normalEvent.post(5678)
                    fragment.viewModel.normalEvent.post(9012)
                    assertEquals(0, fragment.eventResults.size)
                    fragment.lifecycle.removeObserver(this)
                }
            })
        }
        scenario.recreate()
        scenario.onActivity { activity ->
            val fragment = activity.fragment
            assertEquals(3, fragment.eventResults.size)
            assertEquals(1234, fragment.eventResults[0])
            assertEquals(5678, fragment.eventResults[1])
            assertEquals(9012, fragment.eventResults[2])
        }
    }

    @Test
    fun noOpAfterDestroy() {
        val scenario = activityScenarioRule.scenario
        lateinit var activity: TestActivity
        // save activity instance in advance because it can't be done after destroyed
        scenario.onActivity { activity = it }
        scenario.moveToState(Lifecycle.State.DESTROYED)
        val fragment = activity.fragment
        fragment.viewModel.normalEvent.post(1234)
        assertEquals(0, fragment.eventResults.size)
    }

    @Test
    fun recentHandlerReplacesPreviousOne() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            val fragment = activity.fragment
            fragment.viewModel.handledManyTimesEvent.post(1234)
            fragment.viewModel.handledManyTimesEvent.post(5678)
            assertEquals(0, fragment.eventResults.size)
            assertEquals(2, fragment.eventResults2.size)
            assertEquals(1234, fragment.eventResults2[0])
            assertEquals(5678, fragment.eventResults2[1])
        }
    }

    @Test
    fun ignoreEventsBeforeFirstHandling() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            val fragment = activity.fragment
            fragment.viewModel.ignoredBeforeFirstHandlingEvent.post(1234)
            fragment.viewModel.ignoredBeforeFirstHandlingEvent.post(5678)
            assertEquals(0, fragment.eventResults.size)
        }
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            val fragment = activity.fragment
            assertEquals(0, fragment.eventResults.size)
            fragment.viewModel.ignoredBeforeFirstHandlingEvent.post("foo")
            fragment.viewModel.ignoredBeforeFirstHandlingEvent.post("bar")
            assertEquals(2, fragment.eventResults.size)
            assertEquals("foo", fragment.eventResults[0])
            assertEquals("bar", fragment.eventResults[1])
        }
    }

    class TestActivity : AppCompatActivity() {
        lateinit var fragment: TestFragment

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            if (savedInstanceState == null) {
                fragment = TestFragment()
                supportFragmentManager.beginTransaction()
                        .add(fragment, "TestFragment")
                        .commitNow()
            } else {
                fragment = supportFragmentManager
                        .findFragmentByTag("TestFragment") as? TestFragment
                        ?: throw IllegalStateException()
            }
        }
    }

    class TestFragment : Fragment() {
        val viewModel: TestViewModel by viewModels()

        val eventResults = mutableListOf<Any>()
        val eventResults2 = mutableListOf<Any>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            viewModel.run {
                handle(normalEvent) {
                    eventResults += it
                }
                handle(handledManyTimesEvent) {
                    eventResults += it
                }
            }
        }

        override fun onStart() {
            super.onStart()
            viewModel.run {
                handle(handledManyTimesEvent) {
                }
            }
        }

        override fun onResume() {
            super.onResume()
            viewModel.run {
                handle(handledManyTimesEvent) {
                    eventResults2 += it
                }
            }
        }

        override fun onPause() {
            viewModel.run {
                handle(ignoredBeforeFirstHandlingEvent) {
                    eventResults += it
                }
            }
            super.onPause()
        }
    }

    class TestViewModel : ViewModel() {
        val normalEvent = ViewModelEvent<Any>()
        val handledManyTimesEvent = ViewModelEvent<Any>()
        val ignoredBeforeFirstHandlingEvent = ViewModelEvent<Any>()
    }
}
