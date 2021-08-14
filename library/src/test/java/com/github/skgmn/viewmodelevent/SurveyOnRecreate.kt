package com.github.skgmn.viewmodelevent

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SurveyOnRecreate {
    @get:Rule
    val activityScenarioRule = activityScenarioRule<TestActivity>()

    @Test
    fun rerunOnRecreate() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        var activity = scenario.getActivity()

        val deferred = async { activity.viewModel.rerunSurvey.ask(7).first() }
        assertFalse(deferred.isCompleted)

        scenario.recreate()
        activity = scenario.getActivity()

        assertFalse(deferred.isCompleted)

        activity.viewResponse.emit(9)

        assertEquals("63", deferred.await())
    }

    @Test
    fun cancelOnRecreate() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        var activity = scenario.getActivity()

        val deferred = async { activity.viewModel.cancelSurvey.ask(7).first() }
        assertFalse(deferred.isCompleted)

        scenario.recreate()
        activity = scenario.getActivity()

        assertTrue(deferred.isCancelled)
    }

    @Test
    fun dontRerunAfterCancelledInRestartMode() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        var activity = scenario.getActivity()

        val deferred = async { activity.viewModel.rerunErrorSurvey.ask(7).first() }
        assertTrue(activity.errorSurveyRan)
        assertFalse(deferred.isCompleted)

        activity.viewErrorResponse.emit(CancellationException())
        assertTrue(deferred.isCompleted)

        scenario.recreate()
        activity = scenario.getActivity()

        assertFalse(activity.errorSurveyRan)
    }

    @Test
    fun dontRerunAfterCancelledInAbortMode() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        var activity = scenario.getActivity()

        val deferred = async { activity.viewModel.cancelErrorSurvey.ask(7).first() }
        assertTrue(activity.errorSurveyRan)
        assertFalse(deferred.isCompleted)

        activity.viewErrorResponse.emit(CancellationException())
        assertTrue(deferred.isCompleted)

        scenario.recreate()
        activity = scenario.getActivity()

        assertFalse(activity.errorSurveyRan)
    }

    @Test
    fun dontRerunAfterErrorInRestartMode() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        var activity = scenario.getActivity()

        val deferred = async { activity.viewModel.rerunErrorSurvey.ask(7).first() }
        assertTrue(activity.errorSurveyRan)
        assertFalse(deferred.isCompleted)

        activity.viewErrorResponse.emit(RuntimeException())
        assertTrue(deferred.isCompleted)

        scenario.recreate()
        activity = scenario.getActivity()

        assertFalse(activity.errorSurveyRan)
    }

    @Test
    fun dontRerunAfterErrorInAbortMode() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        var activity = scenario.getActivity()

        val deferred = async { activity.viewModel.cancelErrorSurvey.ask(7).first() }
        assertTrue(activity.errorSurveyRan)
        assertFalse(deferred.isCompleted)

        activity.viewErrorResponse.emit(RuntimeException())
        assertTrue(deferred.isCompleted)

        scenario.recreate()
        activity = scenario.getActivity()

        assertFalse(activity.errorSurveyRan)
    }

    class TestActivity : AppCompatActivity() {
        val viewModel: TestViewModel by viewModels()

        lateinit var viewResponse: MutableSharedFlow<Int>
        lateinit var viewErrorResponse: MutableSharedFlow<Throwable>
        var errorSurveyRan = false

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            viewResponse = MutableSharedFlow(extraBufferCapacity = 1)
            viewErrorResponse = MutableSharedFlow(extraBufferCapacity = 1)

            answer(viewModel.rerunSurvey, DeliveryMode.ALL, OnRecreate.RERUN) {
                (it * viewResponse.first()).toString()
            }
            answer(viewModel.cancelSurvey, DeliveryMode.ALL, OnRecreate.CANCEL) {
                (it * viewResponse.first()).toString()
            }
            answer(viewModel.rerunErrorSurvey, DeliveryMode.ALL, OnRecreate.RERUN) {
                errorSurveyRan = true
                throw viewErrorResponse.first()
            }
            answer(viewModel.cancelErrorSurvey, DeliveryMode.ALL, OnRecreate.CANCEL) {
                errorSurveyRan = true
                throw viewErrorResponse.first()
            }
        }
    }

    class TestViewModel : ViewModel() {
        val rerunSurvey = publicSurvey<Int, String>()
        val cancelSurvey = publicSurvey<Int, String>()
        val rerunErrorSurvey = publicSurvey<Int, String>()
        val cancelErrorSurvey = publicSurvey<Int, String>()
    }
}