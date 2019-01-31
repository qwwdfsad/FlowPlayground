package flow


inline fun <T> flow(crossinline block: suspend FlowSubscription<T>.() -> Unit) = object : Flow<T> {
    override suspend fun consumeEach(consumer: FlowSubscription<T>) = consumer.block()
}

fun <T> Iterable<T>.asFlow(): Flow<T> = flow {
    forEach { value ->
        push(value)
    }
}