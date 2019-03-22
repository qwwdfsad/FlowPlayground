package kotlinx.coroutines.flow.builders

import kotlinx.coroutines.flow.*

/**
 * Creates flow that produces values from a given range.
 */
public fun IntRange.asFlow(): Flow<Int> = flow {
    forEach { value ->
        emit(value)
    }
}

/**
 * Creates flow that produces values from a given range.
 */
public fun LongRange.asFlow(): Flow<Long> = flow {
    forEach { value ->
        emit(value)
    }
}
