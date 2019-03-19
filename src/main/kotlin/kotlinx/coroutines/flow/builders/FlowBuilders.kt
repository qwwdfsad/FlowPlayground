@file:UseExperimental(ExperimentalTypeInference::class)

package kotlinx.coroutines.flow.builders

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
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
 */
public inline fun <T : Any> flow(@BuilderInference crossinline block: suspend FlowCollector<T>.() -> Unit): Flow<T> {
    return object : Flow<T> {
        override suspend fun collect(collector: FlowCollector<T>) = collector.block()
    }
}

/**
 * Creates flow that produces single value from a given functional type.
 */
public fun <T : Any> (() -> T).asFlow(): Flow<T> = flow {
    emit(invoke())
}

/**
 * Creates flow that produces single value from a given functional type.
 */
public fun <T : Any> (suspend () -> T).asFlow(): Flow<T> = flow {
    emit(invoke())
}

/**
 * Creates flow that produces values from a given iterable.
 */
public fun <T : Any> Iterable<T>.asFlow(): Flow<T> = flow {
    forEach { value ->
        emit(value)
    }
}

/**
 * Creates flow that produces values from a given array of elements.
 */

public fun <T : Any> flowOf(vararg elements: T): Flow<T> = elements.asIterable().asFlow()
