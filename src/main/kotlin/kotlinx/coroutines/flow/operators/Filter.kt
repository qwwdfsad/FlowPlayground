package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

/**
 * Transforms the given flow into a flow of elements that match given [predicate]
 * TODO inline after bug fixing in crossinline
 */
public fun <T : Any> Flow<T>.filter(predicate: suspend (T) -> Boolean): Flow<T> = flow {
    collect { value ->
        if (predicate(value)) emit(value)
    }
}