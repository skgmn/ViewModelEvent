package com.github.skgmn.viewmodelevent

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MultipleScreenAnswerSurvey {
    @get:Rule
    val activityScenarioRule = activityScenarioRule<TestActivity>()

    @Test
    fun multipleHandling() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        val activity = scenario.getActivity()

        val deferred1 = async { activity.viewModel.normalSurvey.ask(7).toList() }
        val deferred2 = async { activity.viewModel.normalSurvey.ask(11).toList() }
        val deferred3 = async { activity.viewModel.normalSurvey.ask(13).toList() }

        val list1 = deferred1.await()
        assertEquals(2, list1.size)
        assertTrue("14" in list1)
        assertTrue("21" in list1)

        val list2 = deferred2.await()
        assertEquals(2, list2.size)
        assertTrue("22" in list2)
        assertTrue("33" in list2)

        val list3 = deferred3.await()
        assertEquals(2, list3.size)
        assertTrue("26" in list3)
        assertTrue("39" in list3)
    }

    @Test
    fun differentLifecycle() = runBlockingTest {
        val scenario = activityScenarioRule.scenario
        val activity = scenario.getActivity()

        activity.supportFragmentManager.beginTransaction()
            .detach(activity.fragment2)
            .commitNow()

        val list1 = mutableListOf<String>()
        val list2 = mutableListOf<String>()
        val list3 = mutableListOf<String>()

        val job1 = launch {
            activity.viewModel.normalSurvey.ask(7).collect { list1 += it }
        }
        val job2 = launch {
            activity.viewModel.normalSurvey.ask(11).collect { list2 += it }
        }
        val job3 = launch {
            activity.viewModel.normalSurvey.ask(13).collect { list3 += it }
        }

        assertFalse(job1.isCompleted)
        assertFalse(job2.isCompleted)
        assertFalse(job3.isCompleted)

        assertEquals(1, list1.size)
        assertEquals("14", list1[0])

        assertEquals(1, list2.size)
        assertEquals("22", list2[0])

        assertEquals(1, list3.size)
        assertEquals("26", list3[0])

        activity.supportFragmentManager.beginTransaction()
            .attach(activity.fragment2)
            .commitNow()

        assertTrue(job1.isCompleted)
        assertTrue(job2.isCompleted)
        assertTrue(job3.isCompleted)

        assertEquals(2, list1.size)
        assertEquals("21", list1[1])

        assertEquals(2, list2.size)
        assertEquals("33", list2[1])

        assertEquals(2, list3.size)
        assertEquals("39", list3[1])
    }

    class TestActivity : AppCompatActivity() {
        val viewModel: TestViewModel by viewModels()

        val fragment1 = TestFragment().apply { multiplier = 2 }
        val fragment2 = TestFragment().apply { multiplier = 3 }

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

        var multiplier: Int = 2

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            answer(viewModel.normalSurvey, DeliveryMode.ALL) {
                (it * multiplier).toString()
            }
        }
    }

    class TestViewModel : ViewModel() {
        val normalSurvey = publicSurvey<Int, String>()
    }
}