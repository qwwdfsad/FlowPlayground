@file:UseExperimental(ExperimentalTypeInference::class)

package kotlinx.coroutines.flow.builders

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
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
 */
public inline fun <T : Any> flow(@BuilderInference crossinline block: suspend FlowCollector<T>.() -> Unit) = object : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) = collector.block()
}

/**
 * Creates flow from a given suspendable [block] that will be executed within provided [flowContext].
 * Throws [IllegalStateException] if [flowContext] contains [Job] element, all parent-child relationship
 * and cancellation should be controlled by downstream.
 */
public inline fun <T : Any> flow(flowContext: CoroutineContext, @BuilderInference crossinline block: suspend FlowCollector<T>.() -> Unit): Flow<T> =
    flow(block).withUpstreamContext(flowContext)

/**
 * Creates flow that produces single value from a given functional type.
 */
public fun <T : Any> (() -> T).flow(): Flow<T> = SuppliedFlow(this)

private class SuppliedFlow<T : Any>(private val supplier: () -> T) : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) {
        collector.emit(supplier())
    }
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
public fun <T : Any> flow(vararg elements: T) = elements.asIterable().asFlow()
