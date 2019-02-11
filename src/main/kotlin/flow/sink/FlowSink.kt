package flow.sink

import flow.*
import flow.source.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*

public interface FlowSink<T : Any> {

    companion object {
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
    suspend fun join()
}

public enum class BackpressureStrategy {
    ERROR,
    DROP,
    BLOCK,
    BUFFER
}

private class FlowSinkImpl<T : Any>(backpressure: BackpressureStrategy) : FlowSink<T> {

    @JvmField
    internal val channel = Channel<T>()

    override fun onNext(element: T) {
        channel.sendBlocking(element)
    }

    override fun onException(throwable: Throwable) {
        channel.close(throwable)
    }

    override suspend fun join() {
        suspendCancellableCoroutine<Unit> { cc ->
            channel.invokeOnClose { cc.resumeWith(Result.success(Unit)) }
        }
    }
}