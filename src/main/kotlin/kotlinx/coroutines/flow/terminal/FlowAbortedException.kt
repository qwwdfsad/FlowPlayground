package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.*

/**
 * Specific cancellation exception that can be thrown in order to stop consumption of the flow.
 * For example:
 * ```
 * suspend fun <T : Any> Flow<T>.first(): T {
 *     var result: T? = null
 *     try {
 *         collect { value ->
 *             result = value
 *             throw FlowAbortedException()
 *         }
 *         return result ?: throw NoSuchElementException("Flow was empty")
 *      } catch (e: FlowAbortedException) {
 *          return result ?: error("Flow was empty")
 *      }
 * }
 * ```
 */
public class FlowAbortedException : CancellationException("Flow consumer aborted") {
    // Implementation note: cannot be `object` because of `addSuppressed`.
    override fun fillInStackTrace(): Throwable {
        return this
    }
}
