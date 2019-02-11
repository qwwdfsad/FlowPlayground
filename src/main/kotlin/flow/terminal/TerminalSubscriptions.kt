package flow.terminal

import flow.*
import flow.operators.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

// TODO should be inline but triggers BE error
fun <T : Any> Flow<T>.consumeOn(
    context: CoroutineContext, onError: suspend (Throwable) -> Unit = { throw it },
    onComplete: suspend () -> Unit = {}, onNext: suspend (T) -> Unit
): Job = GlobalScope.launch(Dispatchers.Unconfined) {
        try {
            withDownstreamContext(context).flowBridge(onNext)
            onComplete()
        } catch (e: Throwable) {
            onError(e)
            coroutineContext.cancel()
        }
    }

