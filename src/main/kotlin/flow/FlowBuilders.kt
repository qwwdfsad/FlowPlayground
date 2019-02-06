package flow

inline fun <T> flow(crossinline block: suspend FlowSubscriber<T>.() -> Unit) = object : Flow<T> {
    override suspend fun subscribe(consumer: FlowSubscriber<T>) = consumer.block()
}

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
