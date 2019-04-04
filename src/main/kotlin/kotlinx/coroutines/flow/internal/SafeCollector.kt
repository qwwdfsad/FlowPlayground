package kotlinx.coroutines.flow.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.*

@PublishedApi
internal class SafeCollector<T : Any>(
    private val collector: FlowCollector<T>,
    private val ctx: CoroutineContext
) : FlowCollector<T> {

    @Synchronized // Actually we can detect overlapping calls here and trigger error as well
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    override suspend fun emit(value: T) {
        val ctx2 = ctx.minusKey(Job).minusKey(CoroutineId)
        val actual = coroutineContext.minusKey(Job).minusKey(CoroutineId)
        if (ctx2 != actual) {
            error(
                "Flow invariant is violated: flow was collected in $ctx2, but emission happened in $actual. " +
                        "Please refer to 'flow' documentation or use 'flowOn' instead"
            )
        }
        collector.emit(value)
    }
}
