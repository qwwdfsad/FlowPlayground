package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

/**
 * Returns a flow that ignores first [count] elements.
 */
public fun <T : Any> Flow<T>.drop(count: Int): Flow<T> {
    require(count >= 0) { "Drop count should be non-negative, but had $count" }
    return flow {
        var skipped = 0
        collect { value ->
            if (++skipped > count) emit(value)
        }
    }
}


/**
 * Returns a flow containing all elements except first elements that satisfy the given predicate.
 */
public fun <T : Any> Flow<T>.dropWhile(predicate: suspend (T) -> Boolean): Flow<T> = flow {
    var matched = false
    collect { value ->
        if (matched) {
            emit(value)
        } else if (!predicate(value)) {
            matched = true
            emit(value)
        }
    }
}
