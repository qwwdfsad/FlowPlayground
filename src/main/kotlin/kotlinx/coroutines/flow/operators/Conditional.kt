@file:Suppress("UNCHECKED_CAST")

package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

fun <T : Any> Flow<T>.limit(count: Int): Flow<T> {
    require(count > 0) { "limit count should be positive, but had $count" }
    return flow {
        var consumed = 0
        try {
            collect { value ->
                emit(value)

                if (++consumed == count) {
                    throw FlowConsumerAborted()
                }
            }
        } catch (e: FlowConsumerAborted) {
            // Do nothing
        }
    }
}

fun <T : Any> Flow<T>.skip(count: Int): Flow<T> {
    require(count >= 0) { "skip count should be non-negative, but had $count" }
    return flow {
        var skipped = 0
        collect { value ->
            if (++skipped > count) emit(value)
        }
    }
}