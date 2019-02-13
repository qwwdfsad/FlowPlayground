@file:Suppress("unused")

package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlin.coroutines.*

/**
 * Changes upstream context of the flow execution to the given [upstreamContext].
 * [upstreamContext] affects context of every operator and consumer in flow except ones
 * that are below [withDownstreamContext].
 *
 * This operator can be applied to [Flow] only once and all consecutive invocations of [withUpstreamContext]
 * have no effect.
 *
 * Method throws [IllegalStateException] if [upstreamContext] contains [Job] element.
 * It is done in order to prevent accidental mis-usage of this operator, because it effectively
 * prevents propagation of cancellation from the downstream.
 *
 * Example:
 * ```
 * flow // <- context of the flow is changed to foo
 *   .map {} // context of this map is changed to foo
 *   .withUpstreamContext(foo)
 *   .map {} // context of this map is changed to foo as well
 * ```
 */
public fun <T : Any> Flow<T>.withUpstreamContext(upstreamContext: CoroutineContext): Flow<T> {
    val job = upstreamContext[Job]
    if (job != null) {
        error("Upstream context operator should not contain job in it, but had $job")
    }

    return flow {
        withContext(upstreamContext) {
            flowBridge {
                push(it)
            }
        }
    }
}

/**
 * Changes downstream context of the flow execution to the given [downstreamContext].
 * The rule of thumb: the context of all **subsequent** (aka downstream) operators is changed.
 * [bufferSize] controls the size of the backpressure buffer between two contexts.
 *
 * Example:
 * ```
 * flow // some context
 *   .map {} // Some context
 *   .withDownstreamContext(foo)
 *   .map {} // Foo context
 * ```
 */
public fun <T : Any> Flow<T>.withDownstreamContext(downstreamContext: CoroutineContext, bufferSize: Int = 16): Flow<T> =
    flow {
        coroutineScope {
            val parent = coroutineContext[Job]!!
            val channel = produce<T>(capacity = bufferSize) {
                try {
                    flowBridge {
                        channel.send(it)
                    }
                } catch (e: CancellationException) {
                    // TODO discuss it
                    val child = coroutineContext[Job]!!
                    child.invokeOnCompletion(onCancelling = true) { parent.cancel() }
                }
            }

            withContext(downstreamContext) {
                // TODO investigate cancellability issues
                for (value in channel) {
                    push(value)
                }
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
