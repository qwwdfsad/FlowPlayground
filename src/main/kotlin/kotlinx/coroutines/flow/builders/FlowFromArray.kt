package kotlinx.coroutines.flow.builders

import kotlinx.coroutines.flow.*

/**
 * Creates flow that produces values from a given array.
 */
public fun <T: Any> Array<T>.asFlow(): Flow<T> = flow {
    forEach { value ->
        emit(value)
    }
}

/**
 * Creates flow that produces values from a given array.
 */
public fun IntArray.asFlow(): Flow<Int> = flow {
    forEach { value ->
        emit(value)
    }
}

/**
 * Creates flow that produces values from a given array.
 */
public fun LongArray.asFlow(): Flow<Long> = flow {
    forEach { value ->
        emit(value)
    }
}
