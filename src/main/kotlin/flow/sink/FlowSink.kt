@file:UseExperimental(ExperimentalTypeInference::class)

package flow.sink

import flow.*
import flow.source.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*
import kotlin.experimental.*

/**
 * Bridge interface for non-suspending and suspending worlds.
 * Main usage is to create flows from callback-based API
 */
public interface FlowSink<T : Any> {

    companion object {

        // TODO ugly
        @BuilderInference
        public fun <T : Any> create(
            backpressure: BackpressureStrategy = BackpressureStrategy.BLOCK,
            block: suspend (FlowSink<T>) -> Unit
        ): Flow<T> = flow {
            val sink = FlowSinkImpl<T>(backpressure)
            GlobalScope.launch(coroutineContext) {
                block(sink)
            }

            try {
                for (element in sink.channel) {
                    push(element)
                }
            } catch (e: CancellationException) {
                sink.channel.cancel()
            }
        }
    }

    fun onNext(element: T)

    fun onException(throwable: Throwable)

    fun onCompleted()

    suspend fun join()
}

public enum class BackpressureStrategy {
    ERROR,
    DROP,
    LATEST,
    BLOCK,
    BUFFER
}

private class FlowSinkImpl<T : Any>(backpressure: BackpressureStrategy) : FlowSink<T> {

    @JvmField
    internal val channel = Channel<T>()

    override fun onNext(element: T) {
        // TODO switch mode
        channel.sendBlocking(element)
    }

    override fun onException(throwable: Throwable) {
        channel.close(throwable)
    }

    override fun onCompleted() {
        channel.close()
    }

    override suspend fun join() {
        suspendCancellableCoroutine<Unit> { cc ->
            channel.invokeOnClose { cc.resumeWith(Result.success(Unit)) }
        }
    }
}
