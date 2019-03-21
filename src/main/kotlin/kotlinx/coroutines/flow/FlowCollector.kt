package kotlinx.coroutines.flow

import kotlin.coroutines.*

/**
 * Collector of the flow, used as an intermediate or a terminal consumer of the flow.
 * This interface usually should not be implemented directly, but rather used as a receiver in [flow] builder when implementing a custom operator.
 * Implementations of this interface are not thread safe.
 */
public interface FlowCollector<T: Any> {

    /**
     * Emits the current value into a downstream.
     * TODO carefully explain in detail what this method does?
     */
    public suspend fun emit(value: T)
}

// Just an additional protection layer
@Deprecated(message = "withContext in flow body is deprecated, use flowOn instead", level = DeprecationLevel.ERROR)
public fun <T : Any, R> FlowCollector<T>.withContext(context: CoroutineContext, block: suspend () -> R): Unit = error("Should not be called")
