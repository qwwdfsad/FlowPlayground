package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import kotlin.coroutines.*

/**
 * Operator that changes the context where this flow is executed to the given [flowContext].
 * This operator is pure as opposed to reactive `subscribeOn` analogues: it **does not** change context of the downstream flow.
 * This operator is composable and affects only precedent operators that do not have its own context.
 *
 * Example:
 * ```
 * val singleValue =
 *  intFlow // will be executed on IO if context wasn't specified before
 *   .map {} // Will be executed in IO
 *   .flowOn(Dispatchers.IO)
 *   .filter {} // Will be executed in Default
 *   .flowOn(Dispatchers.Default)
 *   .single() // Will be executed in the context of the caller
 * ```
 *
 * This operator uses channel of the specific [bufferSize] in order to switch between contexts,
 * but it is not guaranteed that channel will be created, implementation is free to optimize it away in case of fusing.
 *
 * @throws [IllegalStateException] if provided context contains [Job] instance.
 */
public fun <T : Any> Flow<T>.flowOn(flowContext: CoroutineContext, bufferSize: Int = 16): Flow<T> {
    check(flowContext, bufferSize)
    return flow {
        // TODO optimize on similar context/dispatcher
        coroutineScope {
            // TODO default dispatcher is not the best option here
            val channel = produce(flowContext, capacity = bufferSize) {
                collect { value ->
                    send(value)
                }
            }

            // TODO semantics doesn't play well here and we pay for that with additional object
            // (channel as Job).invokeOnCompletion { if (it is CancellationException && it.cause == null) cancel() }
            for (element in channel) {
                emit(element)
            }

            val producer = channel as Job
            if (producer.isCancelled) {
                producer.join()
                throw producer.getCancellationException()
            }
        }
    }
}

/**
 * Operator that changes the context where all transformations applied to the given flow within a [builder] are executed.
 * This operator is pure and does not affect context of the precedent and subsequent operators.
 *
 * Example:
 * ```
 * flow // ot affected
 *   .map {} // Not affected
 *   .flowWith(Dispatchers.IO) {
 *      map {} // in IO
 *      .filter{} // in IO
 *   }
 *   .map {} // Not affected
 *   .flowOn(Dispatchers.IO)
 * ```
 *
 * This operator uses channel of the specific [bufferSize] in order to switch between contexts,
 * but it is not guaranteed that channel will be created, implementation is free to optimize it away in case of fusing.
 *
 * @throws [IllegalStateException] if provided context contains [Job] instance.
 */
public fun <T : Any, R : Any> Flow<T>.flowWith(
    flowContext: CoroutineContext,
    bufferSize: Int = 16,
    builder: Flow<T>.() -> Flow<R>
): Flow<R> {
    check(flowContext, bufferSize)
    // TODO optimize
    return flow {
        /**
         * Here we should subtract Job instance from context.
         * All builders are written using scoping and no global coroutines are launched,
         * so it is safe. It is also necessary not to mess with cancellations
         * if multiple flowWith are used.
         */
        val originalContext = coroutineContext.minusKey(Job)
        val prepared = this@flowWith.flowOn(originalContext, bufferSize)
        builder(prepared).flowOn(flowContext, bufferSize).collect { value ->
            emit(value)
        }
    }
}

private fun check(flowContext: CoroutineContext, bufferSize: Int) {
    require(flowContext[Job] == null) {
        "Flow context cannot contain job in it. Had $flowContext"
    }

    require(bufferSize >= 0) {
        "Buffer size should be positive, but was $bufferSize"
    }
}

// TODO describe why we don't like this naming
@Deprecated(level = DeprecationLevel.ERROR, message = "Use flowWith or flowOn instead")
public fun <T: Any> Flow<T>.subscribeOn(context: CoroutineContext): Flow<T> = TODO("Should not be called")

@Deprecated(level = DeprecationLevel.ERROR, message = "Use flowWith or flowOn instead")
public fun <T: Any> Flow<T>.observeOn(context: CoroutineContext): Flow<T> = TODO("Should not be called")

@Deprecated(level = DeprecationLevel.ERROR, message = "Use flowWith or flowOn instead")
public fun <T: Any> Flow<T>.publishOn(context: CoroutineContext): Flow<T> = TODO("Should not be called")
