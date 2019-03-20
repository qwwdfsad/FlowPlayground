@file:Suppress("UNCHECKED_CAST")

package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.flow.*

/**
 * Terminal flow operator that collects the given flow with a provided [action].
 * If any exception occurs during collect or in the provided flow, this exception is rethrown from
 * this method.
 *
 * Example of use:
 * ```
 * val flow = getMyEvents()
 * flow.collect { value ->
 *   println("Received $value")
 * }
 * println("My events are consumed")
 * ```
 */
public suspend fun <T : Any> Flow<T>.collect(action: suspend (value: T) -> Unit): Unit =
    collect(object : FlowCollector<T> {
        override suspend fun emit(value: T) = action(value)
    })
