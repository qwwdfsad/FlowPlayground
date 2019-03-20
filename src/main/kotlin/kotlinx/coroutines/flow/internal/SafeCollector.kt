package kotlinx.coroutines.flow.internal

import kotlinx.coroutines.flow.*
import kotlin.coroutines.*

@PublishedApi
internal class SafeCollector<T : Any>(
    private val collector: FlowCollector<T>,
    private val interceptor: ContinuationInterceptor?
) : FlowCollector<T> {

    @Synchronized // Actually we can detect overlapping calls here and trigger error as well
    override suspend fun emit(value: T) {
        if (interceptor != coroutineContext[ContinuationInterceptor]) {
            error(
                "Flow invariant is violated: flow was collected in $interceptor, but emission happened in ${coroutineContext[ContinuationInterceptor]}. " +
                        "Please refer to 'flow' documentation or use 'flowOn' instead"
            )
        }
        collector.emit(value)
    }
}
