package flow

import flow.operators.*

interface Flow<T> {
    suspend fun subscribe(consumer: FlowSubscriber<T>)
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
