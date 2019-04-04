@file:UseExperimental(ExperimentalTypeInference::class)

package kotlinx.coroutines.flow.builders

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.internal.*
import kotlin.coroutines.*
import kotlin.experimental.*

/**
 * Creates flow from a given suspendable [block].
 *
 * Example of usage:
 * ```
 * fun fibonacci(): Flow<Long> = flow {
 *   emit(1L)
 *   var f1 = 1L
 *   var f2 = 1L
 *   repeat(100) {
 *     var tmp = f1
 *     f1 = f2
 *     f2 += tmp
 *     emit(f1)
 *   }
 * }
 * ```
 *
 * `emit` should happen strictly in the dispatchers of the [block] in order to preserve flow purity.
 * For example, the following code will produce [IllegalStateException]:
 * ```
 * flow {
 *   emit(1) // Ok
 *   withContext(Dispatcher.IO) {
 *       emit(2) // Will fail with ISE
 *   }
 * }
 * ```
 * If you want to switch the context where this flow is executed use [flowOn] operator.
 */
public fun <T : Any> flow(@BuilderInference block: suspend FlowCollector<T>.() -> Unit): Flow<T> {
    return object : Flow<T> {
        override suspend fun collect(collector: FlowCollector<T>) {
            SafeCollector(collector, coroutineContext).block()
        }
    }
}

/**
 * Analogue of [flow] builder that does not check a context of flow execution.
 * Used in our own operators where we trust the context of the invocation.
 */
@PublishedApi
internal fun <T : Any> unsafeFlow(@BuilderInference block: suspend FlowCollector<T>.() -> Unit): Flow<T> {
    return object : Flow<T> {
        override suspend fun collect(collector: FlowCollector<T>) {
            collector.block()
        }
    }
}

/**
 * Creates flow that produces single value from a given functional type.
 */
public fun <T : Any> (() -> T).asFlow(): Flow<T> = unsafeFlow {
    emit(invoke())
}

/**
 * Creates flow that produces single value from a given functional type.
 */
public fun <T : Any> (suspend () -> T).asFlow(): Flow<T> = unsafeFlow {
    emit(invoke())
}

/**
 * Creates flow that produces single value from a given functional type.
 */
public fun <T : Any> flowOf(supplier: (suspend () -> T)): Flow<T> = supplier.asFlow()

/**
 * Creates flow that produces values from a given iterable.
 */
public fun <T : Any> Iterable<T>.asFlow(): Flow<T> = unsafeFlow {
    forEach { value ->
        emit(value)
    }
}

/**
 * Creates flow that produces values from a given iterable.
 */
public fun <T : Any> Iterator<T>.asFlow(): Flow<T> = unsafeFlow {
    forEach { value ->
        emit(value)
    }
}


/**
 * Creates flow that produces values from a given sequence.
 */
public fun <T : Any> Sequence<T>.asFlow(): Flow<T> = unsafeFlow {
    forEach { value ->
        emit(value)
    }
}

/**
 * Creates flow that produces values from a given array of elements.
 */
public fun <T : Any> flowOf(vararg elements: T): Flow<T> = elements.asIterable().asFlow()
