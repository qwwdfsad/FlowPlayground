package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import java.lang.AssertionError

/**
 * Concatenates values of each flow sequentially, without interleaving them.
 */
public fun <T : Any> Flow<Flow<T>>.concatenate(): Flow<T> = flow {
    collect {
        val inner = it
        inner.collect { value ->
            emit(value)
        }
    }
}

/**
 * Transforms each value of the given flow into flow of another type and then flattens these flows
 * sequentially, without interleaving them.
 */
public fun <T : Any, R : Any> Flow<T>.concatenate(mapper: suspend (T) -> Flow<R>): Flow<R> = flow {
    collect { value ->
        mapper(value).collect { innerValue ->
            emit(innerValue)
        }
    }
}

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Flow analogue is named concatenate",
    replaceWith = ReplaceWith("concatenate()")
)
public fun <T : Any> Flow<T>.concat(): Flow<T> = error("Should not be called")

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Flow analogue is named concatenate",
    replaceWith = ReplaceWith("concatenate(mapper)")
)
public fun <T : Any,  R : Any> Flow<T>.concatMap(mapper: (T) -> Flow<R>): Flow<R> = error("Should not be called")