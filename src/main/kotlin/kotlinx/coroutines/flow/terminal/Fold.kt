@file:Suppress("UNCHECKED_CAST")

package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.flow.*

/**
 * Accumulates value starting with [initial] value and applying [operation] current accumulator value and each element
 */
public suspend inline fun <T : Any, R> Flow<T>.fold(
    initial: R,
    crossinline operation: suspend (acc: R, value: T) -> R
): R {
    var accumulator = initial
    collect { value ->
        accumulator = operation(accumulator, value)
    }
    return accumulator
}


