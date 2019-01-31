package flow


interface Flow<T> {
    suspend fun consumeEach(consumer: FlowSubscription<T>)
}


@PublishedApi
internal object FlowConsumerAborted : Throwable("Flow consumer aborted", null, false, false)

suspend inline fun <T> Flow<T>.consumeEachWhile(crossinline action: suspend (T) -> Boolean): Boolean = try {
    consumeEach { value ->
        if (!action(value)) throw FlowConsumerAborted
    }
    true
} catch (e: FlowConsumerAborted) {
    false
}

suspend fun <T, C : MutableCollection<in T>> Flow<T>.toCollection(destination: C): C {
    consumeEach { value ->
        destination.add(value)
    }
    return destination
}
