package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*

/**
 * Transforms values emitted by the given flow with [transformer]
 *
 * TODO this method will be inline as soon as all bugs in crossinliner will be fixed
 */
public fun <T : Any, R : Any> Flow<T>.map(transformer: suspend (value: T) -> R): Flow<R> = transform { value -> emit(transformer(value)) }
