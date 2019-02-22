@file:UseExperimental(ExperimentalTypeInference::class)

package kotlinx.coroutines.flow.sink

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlin.experimental.*

/**
 * Bridge interface for non-suspending and suspending worlds to flowViaSink flows from callback-based API.
 * This interface is the direct analogue of [FluxSink] and should not be implemented directly.
 * Use [flowViaSink] instead.
 *
 * See example of usage in `FlowFromCallback.kt`
 */
public interface FlowSink<T : Any> {
    /**
     * Offers a [value] to a downstream flow.
     * Returns `true` if value was successfully emitted into downstream `false` otherwise.
     *
     * This method outcome is tied up with the way this sink was created and users should refer to a factory documentation.
     * For example, if sink was created with `flowViaSink(buffer = Channels.UNLIMITED)`,
     * offer always returns `true`.
     */
    public fun offer(value: T): Boolean

    /**
     * Completes this sink with the given [error], that is rethrown to the flow consumer.
     * Consecutive calls to [offer], [complete] and [error] are ignored.
     */
    public fun completeExceptionally(error: Throwable)

    /**
     * Completes this sink successfully, ending the downstream flow.
     * Consecutive calls to [offer], [complete] and [completeExceptionally] are ignored.
     */
    public fun complete()

    /**
     * Suspends until either [complete] or [completeExceptionally] are invoked from sink side,
     * or terminal flow operator completes.
     */
    public suspend fun join()
}

/**
 * Creates an instance of the cold [Flow] from a supplied sink.
 *
 * To control backpressure, [bufferSize] is used and matches directly to the `capacity` parameter of [Channel]
 * factory. Resulting channel is later used by sink to communicate with flow and its buffer determines
 * backpressure buffer size or its behaviour (e.g. in case when [Channel.CONFLATED] was used).
 *
 * Provided [FlowSink] is thread-safe.
 *
 * Example of usage:
 * ```
 * fun flowFrom(api: CallbackBasedApi): Flow<Int> = flowViaSink { sink ->
 *  val adapter = FlowSinkAdapter(sink) // implementation of callback interface
 *  api.register(adapter)
 *  sink.join() // wait until producer or consumer is done
 *  api.unregister(adapter)
 * }
 * ```
 */
public fun <T : Any> flowViaSink(
    bufferSize: Int = 16,
    @BuilderInference block: suspend (FlowSink<T>) -> Unit
): Flow<T> {
    require(bufferSize >= 0) { "Buffer size should be positive, but was $bufferSize" }
    return flow {
        coroutineScope {
            val sink = FlowSinkImpl<T>(bufferSize)
            launch {
                block(sink)
            }

            // TODO consumeEach on channel?
            try {
                for (value in sink.channel) {
                    emit(value)
                }
            } finally {
                sink.channel.cancel()
            }
        }
    }
}

private class FlowSinkImpl<T : Any>(
    bufferSize: Int
) : FlowSink<T> {

    @JvmField
    internal val channel = Channel<T>(bufferSize)

    override fun offer(value: T): Boolean {
        return try {
            // TODO other exceptions?
            channel.offer(value)
        } catch (e: CancellationException) {
            false
        }
    }

    override fun completeExceptionally(error: Throwable) {
        channel.close(error)
    }

    override fun complete() {
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
