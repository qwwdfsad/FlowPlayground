@file:Suppress("UNCHECKED_CAST")

package flow.terminal

import flow.*
import flow.operators.*
import java.util.*


suspend fun <T : Any> Flow<T>.toList(): List<T> = toCollection(ArrayList())

suspend fun <T : Any> Flow<T>.toSet(): Set<T> = toCollection(LinkedHashSet())

suspend inline fun <S: Any, T: S> Flow<T>.reduce(crossinline operation: suspend (acc: S, value: T) -> S): S {
    var found = false
    var accumulator: S? = null
    flowBridge { value ->
        accumulator = if (found) {
            operation(accumulator as S, value)
        } else {
            found = true
            value
        }
    }
    if (!found) throw UnsupportedOperationException("Empty flow can't be reduced")
    return accumulator as S
}

suspend inline fun <T : Any, R> Flow<T>.fold(initial: R, crossinline operation: suspend (acc: R, value: T) -> R): R {
    var accumulator = initial
    flowBridge { value ->
        accumulator = operation(accumulator, value)
    }
    return accumulator
}

suspend fun Flow<Int>.sum() = fold(0) { acc, value -> acc + value }


suspend fun <T : Any> Flow<T>.first(): T {
    var result: T? = null
    consumeEachWhile { value ->
        result = value
        false
    }
    return result ?: throw NoSuchElementException("Flow is empty")
}

suspend inline fun <T : Any> Flow<T>.first(crossinline predicate: suspend (T) -> Boolean): T {
    var result: T? = null
    val consumed = consumeEachWhile { value ->
        if (predicate(value)) {
            result = value
            false
        } else
            true
    }

    if (consumed) throw NoSuchElementException("Flow contains no element matching the predicate")
    return result as T
}

@PublishedApi
internal object FlowConsumerAborted : Throwable("Flow consumer aborted", null, false, false)

suspend fun <T : Any, C : MutableCollection<in T>> Flow<T>.toCollection(destination: C): C {
    flowBridge { value ->
        destination.add(value)
    }
    return destination
}

suspend inline fun <T : Any> Flow<T>.consumeEachWhile(crossinline action: suspend (T) -> Boolean): Boolean = try {
    flowBridge { value ->
        if (!action(value)) throw FlowConsumerAborted
    }
    true
} catch (e: FlowConsumerAborted) {
    false
}
