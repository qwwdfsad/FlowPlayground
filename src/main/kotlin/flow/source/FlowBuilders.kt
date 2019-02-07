package flow.source

import flow.*
import flow.operators.*
import kotlin.coroutines.*

fun <T> flow(block: suspend FlowSubscriber<T>.() -> Unit) = object : Flow<T> {
    override suspend fun subscribe(consumer: FlowSubscriber<T>) = consumer.block()
}

fun <T> flow(coroutineContext: CoroutineContext, block: suspend FlowSubscriber<T>.() -> Unit): Flow<T> = flow(block).withUpstreamContext(coroutineContext)

private class SuppliedFlow<T>(private val supplier: () -> T): Flow<T> {
    override suspend fun subscribe(subscriber: FlowSubscriber<T>) {
        subscriber.push(supplier())
    }
}

fun <T> (() -> T).flow(): Flow<T> = SuppliedFlow(this)

fun <T> Iterable<T>.asFlow(): Flow<T> = flow {
    forEach { value ->
        push(value)
    }
}

fun <T> flow(vararg elements: T) = elements.asIterable().asFlow()
