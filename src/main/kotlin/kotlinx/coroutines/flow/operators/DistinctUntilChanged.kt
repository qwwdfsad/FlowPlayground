package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

/**
 * Operator that filters out subsequent repetitions of an value.
 */
public fun <T : Any> Flow<T>.distinctUntilChanged(): Flow<T> = distinctUntilChanged { it }

/**
 * Operator that filters out subsequent repetitions of an value comparing
 * their values by [keySelector] function.
 */
public fun <T : Any, K : Any> Flow<T>.distinctUntilChanged(keySelector: (T) -> K): Flow<T> =
    flow {
        var previousKey: K? = null
        collect { value ->
            val key = keySelector(value)
            if (previousKey != key) {
                previousKey = keySelector(value)
                emit(value)
            }
        }
    }