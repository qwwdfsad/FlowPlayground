@file:UseExperimental(ExperimentalTypeInference::class)

package kotlinx.coroutines.flow.sink

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlin.experimental.*

/**
 * Bridge interface for non-suspending and suspending worlds to create flows from callback-based API.
 * This interface is the direct analogue of [FluxSink] and should not be implemented directly.
 * Use [FlowSink.create][FlowSink.Companion.create] instead.
 * Methods of this interface never throw any exceptions except virtual machine errors.
 *
 * See example of usage in `FlowFromCallback.kt`
 *
 * TODO design: should we add `val isCancelled`? What for?
 */
public interface FlowSink<T : Any> {

    /**
     * Emits next [value]
     */
    public fun next(value: T)

    /**
     * Emits error as terminal state of the sink.
     * Consecutive calls to [next], [completed] and [error] are ignored.
     */
    public fun error(error: Throwable)

    /**
     * Emits event indicating that successful terminal state is reached.
     * Consecutive calls to [next], [completed] and [error] are ignored.
     */
    public fun completed()

    /**
     * Suspends until either [completed] or [error] are invoked from sink side,
     * or [Flow] subscriber completes.
     */
    public suspend fun join()

    public companion object // To write FlowSink.create { ... }
}

/**
 * Creates an instance of cold [Flow] from a supplied sink.
 * To control backpressure, [bufferSize] and [overflowStrategy] are used, where the former
 * determines a number of in-flight requests and the latter determines a behaviour in case of buffer overflow.
 *
 * Provided [FlowSink] **is not thread-safe**.
 *
 * Example of usage:
 * ```
 * fun flowFrom(api: CallbackBasedApi): Flow<Int> = FlowSink.create { sink ->
 *  val adapter = FlowSinkAdapter(sink) <
 *  api.register(adapter)
 *  sink.join() // wait until producer or consumer is done
 *  api.unregister(adapter)
 * }
 * ```
 */
@BuilderInference
public fun <T : Any> FlowSink.Companion.create(
    overflowStrategy: OverflowStrategy = OverflowStrategy.BLOCK,
    bufferSize: Int = 16,
    block: suspend (FlowSink<T>) -> Unit
): Flow<T> {
    require(overflowStrategy == OverflowStrategy.BLOCK) { "Only BLOCK overflow strategy is supported, but had $overflowStrategy" }
    require(bufferSize >= 0) { "Buffer size should be positive, but was $bufferSize" }
    return flow {
        coroutineScope {
            val sink = FlowSinkImpl<T>(overflowStrategy, bufferSize)
            launch {
                block(sink)
            }

            // TODO consumeEach on channel?
            try {
                for (element in sink.channel) {
                    push(element)
                }
            } finally {
                sink.channel.cancel()
            }
        }
    }
}

public enum class OverflowStrategy {
    ERROR,
    DROP,
    LATEST,
    BLOCK,
    BUFFER
}

private class FlowSinkImpl<T : Any>(
    private val backpressure: OverflowStrategy,
    bufferSize: Int
) : FlowSink<T> {

    @JvmField
    internal val channel = Channel<T>(bufferSize)

    override fun next(value: T) {
        try {
            channel.sendBlocking(value)
        } catch (e: CancellationException) {
            // Do nothing, items emitted after cancellation are ignored
        }
    }

    override fun error(error: Throwable) {
        channel.close(error)
    }

    override fun completed() {
        channel.close()
    }

    override suspend fun join() {
        try {
            suspendCancellableCoroutine<Unit> { cc ->
                channel.invokeOnClose { cc.resumeWith(Result.success(Unit)) }
            }
        } catch (e: Throwable) {
            // Join never throws
        }
    }
}
