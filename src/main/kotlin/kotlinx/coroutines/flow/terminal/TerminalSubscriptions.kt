package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import kotlin.coroutines.*

// TODO should be inline but triggers BE error
fun <T : Any> Flow<T>.consumeOn(
    context: CoroutineContext, onError: suspend (Throwable) -> Unit = { throw it },
    onComplete: suspend () -> Unit = {}, onNext: suspend (T) -> Unit
): Job = GlobalScope.launch(context) {
        try {
            collect(onNext)
            onComplete()
        } catch (e: Throwable) {
            onError(e)
            // TODO
            coroutineContext.cancel()
        }
    }

