@file:Suppress("UNCHECKED_CAST")

package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import java.util.concurrent.*

/**
 * Returns a flow that contains first [count] elements.
 * When [count] elements are consumed, the original flow is cancelled.
 */
public fun <T : Any> Flow<T>.take(count: Int): Flow<T> {
    require(count > 0) { "Take count should be positive, but had $count" }
    return flow {
        var consumed = 0
        try {
            collect { value ->
                emit(value)
                if (++consumed == count) {
                    throw TakeLimitException()
                }
            }
        } catch (e: TakeLimitException) {
            // Nothing, bail out
        }
    }
}

/**
 * Returns a flow that contains elements satisfying the given [predicate].
 */
public fun <T : Any> Flow<T>.takeWhile(predicate: suspend (T) -> Boolean): Flow<T> = flow {
    try {
        collect { value ->
            if (predicate(value)) emit(value)
            else throw TakeLimitException()
        }
    } catch (e: TakeLimitException) {
        // Nothing, bail out
    }
}

private class TakeLimitException : CancellationException("Flow limit is reached, cancelling") {
    override fun fillInStackTrace(): Throwable = this
}
