package flow.terminal

import flow.*
import flow.operators.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

fun <T: Any> Flow<T>.consumeOn(
    context: CoroutineContext, onException: suspend (Throwable) -> Unit = { throw it },
    onComplete: suspend () -> Unit = {}, action: suspend (T) -> Unit
): Job = consumeOn(context, BaseFlowConsumer(action, onException, onComplete))

fun <T : Any> Flow<T>.consumeOn(
    context: CoroutineContext, consumer: FlowConsumer<T>
): Job {
    return GlobalScope.launch(Dispatchers.Unconfined) {
        try {
            withDownstreamContext(context).flowBridge(consumer::onNext)
            consumer.onComplete()
        } catch (e: Throwable) {
            consumer.onError(e)
            coroutineContext.cancel()
        }
    }
}

private class BaseFlowConsumer<T : Any>(
    private val onNextHandler: suspend (T) -> Unit,
    private val onErrorHandler: suspend (Throwable) -> Unit,
    private val onCompleteHandler: suspend () -> Unit
) : FlowConsumer<T> {
    override suspend fun onNext(element: T) {
        onNextHandler(element)
    }

    override suspend fun onError(throwable: Throwable) {
        onErrorHandler(throwable)
    }

    override suspend fun onComplete() {
        onCompleteHandler()
    }
}
