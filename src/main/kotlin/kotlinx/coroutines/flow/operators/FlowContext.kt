@file:Suppress("unused")

package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlin.coroutines.*

/**
 * Changes upstream context of the flow execution to the given [upstreamContext].
 * The rule of thumb: the context of all operators **prior** (aka upstream) to this call is changed.
 *
 * Such operator can be applied to [Flow] only once and all consecutive invocations of [withUpstreamContext]
 * have no effect.
 *
 * Example:
 * ```
 * flow // <- context of the flow is changed to `foo
 *   .map {} // context of this map is changed
 *   .withUpstreamContext(foo)
 *   .map {} // context of this map is not changed
 * ```
 */
public fun <T : Any> Flow<T>.withUpstreamContext(upstreamContext: CoroutineContext): Flow<T> =
    flow {
        withContext(upstreamContext) {
            flowBridge {
                push(it)
            }
        }
    }

/**
 * Changes downstream context of the flow execution to the given [downstreamContext].
 * The rule of thumb: the context of all **subsequent** (aka downstream) operators is changed.
 * [bufferSize] controls the size of the backpressure buffer between two contexts
 *
 * Example:
 * ```
 * flow
 *   .map {} // Some context
 *   .withDownstreamContext(foo)
 *   .map {} // Foo context
 *
 * ```
 */
public fun <T : Any> Flow<T>.withDownstreamContext(downstreamContext: CoroutineContext, bufferSize: Int = 16): Flow<T> =
    flow {
        val channel = Channel<T>(bufferSize)
        // TODO fast path when dispatcher is the same
        coroutineScope {
            launch(downstreamContext) {
                for (element in channel) {
                    try {
                        push(element)
                    } catch (e: Throwable) {
                        channel.close(e)
                        throw e // Kill the whole hierarchy
                    }
                }
            }

            try {
                flowBridge {
                    channel.send(it)
                }
            } finally {
                channel.close()
            }
        }
    }


// TODO describe why we don't like this naming
@Deprecated(level = DeprecationLevel.ERROR, message = "Use Flow.withUpstreamContext instead", replaceWith = ReplaceWith("withUpstreamContext(context)"))
public fun <T: Any> Flow<T>.subscribeOn(context: CoroutineContext): Flow<T> = TODO("Should not be called")

@Deprecated(level = DeprecationLevel.ERROR, message = "Use Flow.withUpstreamContext instead", replaceWith = ReplaceWith("withDownstreamContext(context)"))
public fun <T: Any> Flow<T>.observeOn(context: CoroutineContext): Flow<T> = TODO("Should not be called")

@Deprecated(level = DeprecationLevel.ERROR, message = "Use Flow.withUpstreamContext instead", replaceWith = ReplaceWith("withDownstreamContext(context)"))
public fun <T: Any> Flow<T>.publishOn(context: CoroutineContext): Flow<T> = TODO("Should not be called")
