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
 *   push(1L)
 *   var f1 = 1L
 *   var f2 = 1L
 *   repeat(100) {
 *     var tmp = f1
 *     f1 = f2
 *     f2 += tmp
 *     push(f1)
 *   }
 * }
 * ```
 * TODO inline when tests are ready
 */
public fun <T : Any> flow(@BuilderInference block: suspend FlowSubscriber<T>.() -> Unit) = object : Flow<T> {
    override suspend fun subscribe(consumer: FlowSubscriber<T>) = consumer.block()
}

/**
 * Creates flow from a given suspendable [block] that will be executed within provided [flowContext].
 */
public fun <T : Any> flow(flowContext: CoroutineContext, @BuilderInference block: suspend FlowSubscriber<T>.() -> Unit): Flow<T> =
    flow(block).withUpstreamContext(flowContext)


/**
 * Creates flow that produces single value from a given functional type.
 */
public fun <T : Any> (() -> T).flow(): Flow<T> = SuppliedFlow(this)

private class SuppliedFlow<T : Any>(private val supplier: () -> T) : Flow<T> {
    override suspend fun subscribe(consumer: FlowSubscriber<T>) {
        consumer.push(supplier())
    }
}

/**
 * Creates flow that produces values from a given iterable.
 */
public fun <T : Any> Iterable<T>.asFlow(): Flow<T> = flow {
    forEach { value ->
        push(value)
    }
}

/**
 * Creates flow that produces values from a given array of elements.
 */
public fun <T : Any> flow(vararg elements: T) = elements.asIterable().asFlow()
