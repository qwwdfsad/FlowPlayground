package flow.terminal

import flow.*
import flow.operators.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

fun <T: Any> Flow<T>.consumeOn(
    context: CoroutineContext,
    action: suspend (T) -> Unit
): Job = consumeOn(context, { throw it }, {}, action)

fun <T: Any> Flow<T>.consumeOn(
    context: CoroutineContext,
    onException: (Throwable) -> Unit,
    action: suspend (T) -> Unit
): Job = consumeOn(context, onException, {}, action)

fun <T: Any> Flow<T>.consumeOn(
    context: CoroutineContext, onException: (Throwable) -> Unit,
    onComplete: () -> Unit, action: suspend (T) -> Unit
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
