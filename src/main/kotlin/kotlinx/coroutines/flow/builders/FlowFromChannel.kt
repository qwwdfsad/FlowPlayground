@file:UseExperimental(ExperimentalTypeInference::class)

package kotlinx.coroutines.flow.builders

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlin.experimental.*

/**
 * Creates an instance of the cold [Flow] from a supplied [SendChannel].
 *
 * To control backpressure, [bufferSize] is used and matches directly to the `capacity` parameter of [Channel]
 * factory. Resulting channel is later used by sink to communicate with flow and its buffer determines
 * backpressure buffer size or its behaviour (e.g. in case when [Channel.CONFLATED] was used).
 *
 * Example of usage:
 * ```
 * fun flowFrom(api: CallbackBasedApi): Flow<Int> = flowViaChannel { channel ->
 *  val adapter = FlowSinkAdapter(channel) // implementation of callback interface
 *  api.register(adapter)
 *  channel.invokeOnClose {
 *    api.unregister(adapter)
 *  }
 * }
 * ```
 */
public fun <T : Any> flowViaChannel( // TODO bikeshed this naming?
    bufferSize: Int = 16,
    @BuilderInference block: suspend (SendChannel<T>) -> Unit
): Flow<T> {
    require(bufferSize >= 0) { "Buffer size should be positive, but was $bufferSize" }
    return flow {
        coroutineScope {
            val channel = Channel<T>(bufferSize)
            launch {
                block(channel)
            }

            // TODO consumeEach on channel?
            try {
                for (value in channel) {
                    emit(value)
                }
            } finally {
                channel.cancel()
            }
        }
    }
}
