@file:Suppress("UNCHECKED_CAST")

package flow.operators

import flow.*
import flow.source.*
import flow.terminal.*

fun <T : Any> Flow<T>.limit(count: Int): Flow<T> {
    require(count > 0) { "limit count should be positive, but had $count" }
    return flow {
        var consumed = 0
        try {
            flowBridge { value ->
                push(value)

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
        flowBridge { value ->
            if (++skipped > count) push(value)
        }
    }
}