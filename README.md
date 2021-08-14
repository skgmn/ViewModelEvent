# Setup

```
dependencies {
    implementation "com.github.skgmn:viewmodelevent:1.1.0"
}
```
If you don't know how to access to GitHub Packges, please refer to [this](https://gist.github.com/skgmn/79da4a935e904078491e932bd5b327c7).

# Usage

## Basic Usage of Event

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

## Event Lifecycle

### Event delivery occurs only between onStart() and onStop()

The lambda function passed to `handle()` is only invoked when its Activity or Fragment is between onStart() and onStop().

### Events are buffered after onStop()

Events posted after onStop() are buffered until the Activity or Fragment retarts. Only the latest item is buffered if `DeliveryMode.LATEST` is passed to `handle()`(this is the default behavior). Otherwise, all items are buffered if `DeliveryMode.ALL` is passed to `handle()`.

### Events live across Activity recreation

Events posted while Activity recreation can be delivered after it is recreated.

## Multiple handling

An Activity or Fragment can handle multiple events, but cannot handle the same event multiple times. Only the latest handler will work then.
It is OK that several Activities or Fragments handle the same event at the same time. It can be useful when some ViewModels are shared by many Activities or Fragments.

## Survey

A `Survey` is a concept similar to `Event`, but it can reply to its sender.

```kotlin
class MyViewModel : ViewModel() {
    val mySurvey = survey<MyQuestion>()
    
    fun hello() {
        viewModelScope.launch {
            mySurvey.ask(MyQuestion()).collect { myAnswer ->
                doSomethingWith(myAnswer)
            }
        }
    }
}

class MyActivity : AppCompatActivity() {
    private val viewModel: MyViewModel by viewModels()

    fun onCreate(savedInstanceState: Bundle?) {
        answer(viewModel.mySurvey) { question ->
            MyAnswerTo(question)
        }
    }
}
```

Event         | Survey         
--------------|---------------
publicEvent() | publicSurvey()
delivery()    | poll()
event()       | survey()
post()        | ask()
handle()      | answer()

Unlike `Event`, `Survey` use coroutine.
`Survey.ask()` returns `Flow`, and `Survey.answer()` accepts `suspend` lambda function.
`Survey` also has same lifecycle as `Event`.

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
