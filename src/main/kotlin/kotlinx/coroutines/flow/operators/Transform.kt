package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.unsafeFlow as flow // hehe
import kotlinx.coroutines.flow.terminal.*

/**
 * Applies [transformer] function to each value of the given flow.
 * [transformer] is a generic function hat may transform emitted element, skip it or emit it multiple times.
 *
 * This operator is useless by itself, but can be used as a building block of user-specific operators:
 * ```
 * fun Flow<Int>.skipOddAndDuplicateEven(): Flow<Int> = transform { value ->
 *   if (value % 2 == 0) { // Emit only even values, but twice
 *     emit(value)
 *     emit(value)
 *   } // Do nothing if odd
 * }
 * ```
 *
 * TODO this method will be inline as soon as all bugs in crossinliner will be fixed
 * TODO measure performance impact again when inliner is fixed
 */
public fun <T : Any, R : Any> Flow<T>.transform(@BuilderInference transformer: suspend FlowCollector<R>.(value: T) -> Unit): Flow<R> {
    return flow {
        collect { value ->
            transformer(value)
        }
    }
}
