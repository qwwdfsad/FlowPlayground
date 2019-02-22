package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlin.coroutines.*

fun <T : Any> Flow<T>.flowOn(coroutineContext: CoroutineContext): Flow<T> = flow {
    coroutineScope {
        val channel = produce(coroutineContext) {
            collect { value ->
                send(value)
            }
        }

        // TODO same issues as in "with downstream context"
        for (element in channel) {
            emit(element)
        }
    }
}

// TODO inline + crossinline
public fun <T : Any, R : Any> Flow<T>.flowWith(
    flowContext: CoroutineContext,
    builder: Flow<T>.() -> Flow<R>
): Flow<R> =
    flow {
        val originalContext = coroutineContext
        val prepared = this@flowWith.flowOn(originalContext)
        builder(prepared).flowOn(flowContext).collect { value ->
            emit(value)
        }
    }