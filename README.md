# Introduction

MVVM has become one of the most popular architecture for Android app development since Google has actively supported it. With MVVM architecture, it is always recommended to write data-oriented or state-oriented code, rather than event-driven code. Most ideal structure is like this: View subscribes ViewModel's data or states, renders UI, and redraws UI whenever data or states change.

But in practice, sometimes it is inevitable to use event. The most typical example is screen navigation. It seems there are really no ways to start new activity according to data or state.

In this case, mostly common way to achieve this was SingleLiveEvent. But there were some drawbacks to me to use them.

* It was not comfortable to use.
* It lacked features.
* I just didn't like LiveData. I thought it had been technically inferior to RxJava and Flow.
* It was not essentially a class for event messenger. It just feels like improvised.
* Most of all, there was no official aar package uploaded to maven repository...

So I wanted to write my own library and this is the result.

# Features

* Easy to use
* Handle events in Activity or Fragment, post events from ViewModel
* Event handler only runs between onStart() and onStop(), like SingleLiveEvent
* Multiple Activity or Fragment can handle same event at the same time.
* Delivery mode is supported.
  * `LATEST` - Only receive the latest event. This is default behavior because it's pretty enough in general cases.
  * `ALL` - Receive all events. Events will be buffered while Activity or Fragment is stopped.

# Setup

```
dependencies {
    implementation "com.github.skgmn:viewmodelevent:1.0.0"
}
```
If you don't know how to access to GitHub Packges, please refer to [this](https://gist.github.com/skgmn/79da4a935e904078491e932bd5b327c7).

# Usage

## Basic Usage

```kotlin
class MyViewModel : ViewModel() {
    val myEvent = publicEvent<MyEvent>()
    
    fun hello() {
        myEvent.post(MyEvent())
    }
}

class MyActivity : AppCompatActivity() {
    private val viewModel: MyViewModel by viewModels()
    
    fun onCreate(savedInstanceState: Bundle?) {
        handle(viewModel.myEvent) { doSomeWorkWith(it) }
    }
}
```
If you want to inhibit other than MyViewModel from posting events, then create private `Delivery` and post events through it.
```kotlin
class MyViewModel : ViewModel() {
    private val myEventDelivery = delivery<MyEvent>()
    val myEvent = event(myEventDelivery)
    
    private val yourEventDelivery = delivery<YourEvent>()
    val yourEvent = event(yourEventDelivery)
    
    fun hello() {
        myEventDelivery.post(MyEvent())
        yourEventDelivery.post(YourEvent())
    }
}

class MyActivity : AppCompatActivity() {
    private val viewModel: MyViewModel by viewModels()
    
    fun onCreate(savedInstanceState: Bundle?) {
        handle(viewModel.myEvent) { doSomeWorkWith(it) }
        handle(viewModel.yourEvent) { doOtherThingsWith(it) }
    }
}
```
If you are free to make your ViewModel inherit other classes, make it extend `com.github.skgmn.viewmodelevent.ViewModel` rather than `androidx.lifecycle.ViewModel`. It becomes even easier.
```kotlin
class MyViewModel : com.github.skgmn.viewmodelevent.ViewModel() {
    val myEvent = event<MyEvent>()
    
    fun hello() {
        // this post() is invisible to the outside of MyViewModel
        myEvent.post(MyEvent())
    }
}
```

# Implementation Details

* To keep it simple to use, an Activity or a Fragment cannot handle same event multiple times. For example, code below may not work as intended. The latter handler replaces the former one so it only shows dialog.
```kotlin
class MyActivity : Activity() {
    fun onCreate(savedInstanceState: Bundle?) {
        handle(viewModel.myEvent) { showToast() }
        handle(viewModel.myEvent) { showDialog() }
    }
}
```
* Events which are emitted before the first handler is registered are ignored. But since the first handler starts listening, events are buffered when event handler goes off (Activity is stopped). This behavior remains until Activity or Fragment is totally destroyed (ViewModel is cleared).

# License

```
MIT License

Copyright (c) 2021 skgmn

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
