# Introduction

MVVM has become one of the most popular architecture for Android app development since Google has actively supported it. With MVVM architecture, it is always recommended to write data-oriented or state-oriented code, rather than event-driven code. Most ideal structure is like this: View subscribes ViewModel's data or states, renders UI, and redraws UI whenever data or states change.

But in practice, sometimes it is inevitable to use event. The most typical example is screen navigation. It seems there are really no ways to start new activity according to data or state.

In this case, mostly common way to achieve this was SingleLiveEvent. But there were some drawbacks to me to use them.

* It was not comfortable to use.
* It lacked some features like multiple handling, backpressure option, etc.
* I just didn't like LiveData. I thought it had been technically inferior to RxJava and Flow.
* It was not essentially a class for event messenger. It just looked like improvised.
* Most of all, there were no official aar package uploaded to maven repository...

So I wanted to write my own library and this is the result.

# Features

* Handle events in Activity or Fragment, post events from everywhere
* Event handler only runs between onStart() and onStop(), like SingleLiveEvent
* An event can has at most one handler per Activity or Fragment, but multiple handler from multiple Activity or Fragment can be attached, unlike SingleLiveEvent
* Backpressure options are supported.
  * `LATEST` - when events are posted after onStop(), only the latest event is dispatched to handler when it is back to onStart(). This is the default behavior.
  * `BUFFER` - when events are posted after onStop(), all events are buffered and dispatched to handler eventually when it is back to onStart().

# Usage

```kotlin
class MyViewModel : ViewModel() {
    val someEvent = LiveViewEvent<EventType>()
    
    fun hello() {
        someEvent.post(EventType())
    }
}

class MyActivity : AppCompatActivity() {
    private val viewModel: MyViewModel by viewModels()
    
    fun onCreate(savedInstanceState: Bundle?) {
        handle(viewModel.someEvent) {
            doSomeWorkWith(it)
        }
    }
}
```
