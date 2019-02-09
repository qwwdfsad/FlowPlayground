package flow.terminal

import flow.*
import flow.operators.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

fun <T> Flow<T>.consumeOn(context: CoroutineContext, action: suspend (T) -> Unit): Job {
    return GlobalScope.launch(context) {
        flowBridge(action)
    }
}