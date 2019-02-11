@file:Suppress("UNCHECKED_CAST")

package flow.operators

import flow.*
import flow.source.*
import flow.terminal.*

fun <T : Any> Flow<T>.limit(count: Int): Flow<T> = flow {
    var consumed = 0
    try {
        flowBridge { value ->
            if (++consumed > count) throw FlowConsumerAborted
            push(value)
        }
    } catch (e: FlowConsumerAborted) {
        // Do nothing
    }
}

fun <T : Any> Flow<T>.skip(count: Int): Flow<T> = flow {
    var skipped = 0
    flowBridge { value ->
        if (++skipped > count) push(value)
    }
}