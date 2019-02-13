@file:Suppress("UNCHECKED_CAST")

package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import java.util.*
import java.util.concurrent.*

suspend fun <S: Any, T: S> Flow<T>.reduce(operation: suspend (acc: S, value: T) -> S): S {
    var found = false
    var accumulator: S? = null
    flowBridge { value ->
        accumulator = if (found) {
            operation(accumulator as S, value)
        } else {
            found = true
            value
        }
    }
    if (!found) throw UnsupportedOperationException("Empty flow can't be reduced")
    return accumulator as S
}

suspend fun <T : Any, R> Flow<T>.fold(initial: R, operation: suspend (acc: R, value: T) -> R): R {
    var accumulator = initial
    flowBridge { value ->
        accumulator = operation(accumulator, value)
    }
    return accumulator
}


internal class FlowConsumerAborted : CancellationException("Flow consumer aborted") {
    // TODO provide a non-suppressable ctor argument
    override fun fillInStackTrace(): Throwable {
        return this
    }
}
