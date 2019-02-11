package flow.terminal

import flow.*
import flow.operators.*
import kotlinx.coroutines.*
import kotlin.coroutines.*


fun <T: Any> Flow<T>.consumeOn(
    context: CoroutineContext, onException: (Throwable) -> Unit = {throw it},
    onComplete: () -> Unit = {}, action: suspend (T) -> Unit
): Job {
    return GlobalScope.launch(Dispatchers.Unconfined) {
        try {
            withDownstreamContext(context).flowBridge(action)
            onComplete()
        } catch (e: Throwable) {
            onException(e)
        }
    }
}
