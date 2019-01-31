@file:Suppress("UNCHECKED_CAST")

package flow

import io.reactivex.*
import java.util.*
import java.util.concurrent.*


suspend fun <T> Flow<T>.toList(): List<T> = toCollection(ArrayList())

suspend fun <T> Flow<T>.toSet(): Set<T> = toCollection(LinkedHashSet())

suspend inline fun <S, T : S> Flow<T>.reduce(crossinline operation: suspend (acc: S, value: T) -> S): S {
    var found = false
    var accumulator: S? = null
    consumeEach { value ->
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

suspend inline fun <T, R> Flow<T>.fold(initial: R, crossinline operation: suspend (acc: R, value: T) -> R): R {
    var accumulator = initial
    consumeEach { value ->
        accumulator = operation(accumulator, value)
    }
    return accumulator
}

suspend fun Flow<Int>.sum() = fold(0) { acc, value -> acc + value }


suspend fun <T> Flow<T>.first(): T {
    var result: T? = null
    consumeEachWhile { value ->
        result = value
        false
    }
    return result ?: throw NoSuchElementException("Flow is empty")
}

suspend inline fun <T> Flow<T>.first(crossinline predicate: suspend (T) -> Boolean): T {
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
