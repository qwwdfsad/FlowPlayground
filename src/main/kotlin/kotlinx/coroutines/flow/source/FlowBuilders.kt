@file:UseExperimental(ExperimentalTypeInference::class)

package kotlinx.coroutines.flow.source

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import kotlin.coroutines.*
import kotlin.experimental.*

@BuilderInference
fun <T : Any> flow(block: suspend FlowSubscriber<T>.() -> Unit) = object :
    Flow<T> {
    override suspend fun subscribe(consumer: FlowSubscriber<T>) = consumer.block()
}

@BuilderInference
fun <T : Any> flow(coroutineContext: CoroutineContext, block: suspend FlowSubscriber<T>.() -> Unit): Flow<T> =
    flow(block).withUpstreamContext(coroutineContext)

private class SuppliedFlow<T : Any>(private val supplier: () -> T) : Flow<T> {
    override suspend fun subscribe(subscriber: FlowSubscriber<T>) {
        subscriber.push(supplier())
    }
}

fun <T : Any> (() -> T).flow(): Flow<T> = SuppliedFlow(this)

fun <T : Any> Iterable<T>.asFlow(): Flow<T> = flow {
    forEach { value ->
        push(value)
    }
}

fun <T : Any> flow(vararg elements: T) = elements.asIterable().asFlow()
