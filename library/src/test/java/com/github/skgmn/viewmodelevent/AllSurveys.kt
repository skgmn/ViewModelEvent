package com.github.skgmn.viewmodelevent

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AllSurveys {
    @get:Rule
    val activityScenarioRule = activityScenarioRule<TestActivity>()

    @Test
    fun immediateWhenStarted() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        val activity = scenario.getActivity()

        val answer1 = async {
            activity.viewModel.normalSurvey.ask(1234).singleOrNull()
        }
        val answer2 = async {
            activity.viewModel.normalSurvey.ask(5678).singleOrNull()
        }
        val answer3 = async {
            activity.viewModel.normalSurvey.ask(9012).singleOrNull()
        }

        assertEquals("1234", answer1.await())
        assertEquals("5678", answer2.await())
        assertEquals("9012", answer3.await())
    }

    @Test
    fun delayedOnStopStart() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        val activity = scenario.getActivity()

        scenario.moveToState(Lifecycle.State.CREATED)

        val answer1 = async { activity.viewModel.normalSurvey.ask(1234).singleOrNull() }
        val answer2 = async { activity.viewModel.normalSurvey.ask(5678).singleOrNull() }
        val answer3 = async { activity.viewModel.normalSurvey.ask(9012).singleOrNull() }

        assertFalse(answer1.isCompleted)
        assertFalse(answer2.isCompleted)
        assertFalse(answer3.isCompleted)

        scenario.moveToState(Lifecycle.State.STARTED)

        assertTrue(answer1.isCompleted)
        assertTrue(answer2.isCompleted)
        assertTrue(answer3.isCompleted)

        assertEquals("1234", answer1.await())
        assertEquals("5678", answer2.await())
        assertEquals("9012", answer3.await())
    }

    @Test
    fun subscribeAfterStopRecreateRespond(): Unit = runBlockingTest {
        val scenario = activityScenarioRule.scenario

        lateinit var job: Job

        var activity = scenario.getActivity()
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                job = activity.viewModel.viewModelScope.launch {
                    val answer1 = async { activity.viewModel.recreateSurvey.ask(1234).single() }
                    val answer2 = async { activity.viewModel.recreateSurvey.ask(5678).single() }
                    assertEquals("3702", answer1.await())
                    assertEquals("17034", answer2.await())
                }
                activity.lifecycle.removeObserver(this)
            }
        })

        scenario.recreate()
        activity = scenario.getActivity()

        activity.viewResponse.tryEmit(3)

        job.join()
    }

    @Test
    fun subscribeBeforeStopRecreateRespond(): Unit = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        var activity = scenario.getActivity()

        val answer1 = async { activity.viewModel.recreateSurvey.ask(1234).single() }
        val answer2 = async { activity.viewModel.recreateSurvey.ask(5678).single() }

        scenario.recreate()
        activity = scenario.getActivity()

        activity.viewResponse.tryEmit(3)

        assertEquals("3702", answer1.await())
        assertEquals("17034", answer2.await())
    }

    @Test
    fun latterRespondEarlierThanFormer() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        val activity = scenario.getActivity()

        val signal1 = MutableSharedFlow<Int>()
        val signal2 = MutableSharedFlow<Int>()

        val answer1 = async { activity.viewModel.pendingSurvey.ask(signal1).singleOrNull() }
        val answer2 = async { activity.viewModel.pendingSurvey.ask(signal2).singleOrNull() }

        assertFalse(answer1.isCompleted)
        assertFalse(answer2.isCompleted)

        signal2.emit(5678)

        assertFalse(answer1.isCompleted)
        assertTrue(answer2.isCompleted)
        assertEquals("5678", answer2.await())

        signal1.emit(1234)
        assertTrue(answer1.isCompleted)
        assertEquals("1234", answer1.await())
    }

    class TestActivity : AppCompatActivity() {
        val viewModel: TestViewModel by viewModels()

        lateinit var viewResponse: MutableSharedFlow<Int>

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            viewResponse = MutableSharedFlow(extraBufferCapacity = 1)
            answer(viewModel.normalSurvey, DeliveryMode.ALL) {
                it.toString()
            }
            answer(viewModel.pendingSurvey, DeliveryMode.ALL) {
                it.first().toString()
            }
            answer(viewModel.recreateSurvey, DeliveryMode.ALL) {
                (viewResponse.first() * it).toString()
            }
        }
    }

    class TestViewModel : ViewModel() {
        val normalSurvey = publicSurvey<Int, String>()
        val pendingSurvey = publicSurvey<Flow<Int>, String>()
        val recreateSurvey = publicSurvey<Int, String>()
    }
}
