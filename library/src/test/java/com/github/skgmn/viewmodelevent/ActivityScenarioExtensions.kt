package com.github.skgmn.viewmodelevent

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun <A : Activity> ActivityScenario<A>.getActivity(): A {
    return suspendCoroutine { cont ->
        onActivity { activity ->
            cont.resume(activity)
        }
    }
}