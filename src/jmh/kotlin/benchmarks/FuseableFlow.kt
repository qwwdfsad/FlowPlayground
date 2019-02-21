package benchmarks

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.operators.*
import reactor.core.publisher.*
import java.util.*

typealias Transformer<T, R> = suspend (T) -> R?

// TODO it could be non-public
abstract class FuseableFlow<T : Any, R : Any>(private val flow: Flow<T>) : Flow<R> {

    private val transformers = ArrayDeque<FuseableFlow<Any, Any>>(4)
    private val sourceFlow: Flow<*> = nonFuseableFlow(this)

    private tailrec fun nonFuseableFlow(flow: Flow<*>): Flow<*> {
        if (flow !is FuseableFlow<*, *>) {
            return flow
        }

        transformers.addFirst(flow as FuseableFlow<Any, Any>)
        return nonFuseableFlow(flow.flow)
    }

    protected abstract suspend fun transform(value: T): R?

    override suspend fun collect(collector: FlowCollector<R>) {
        if (sourceFlow === flow) {
            flow.collect { value ->
                val actual = transform(value) ?: return@collect
                collector.emit(actual)
            }
            return
        }

        sourceFlow.collect { value ->
            var transformed = value
            for (fuseable in transformers) {
                transformed = fuseable.transform(transformed) ?: return@collect
            }

            collector.emit(transformed as R)
        }

    }
}

inline fun <T : Any, R : Any> Flow<T>.transform(crossinline transformer: Transformer<T, R>): Flow<R> =
    object : FuseableFlow<T, R>(this) {
        override suspend fun transform(value: T): R? {
            return transformer(value)
        }
    }

inline fun <T : Any> Flow<T>.fusedFilter(crossinline predicate: suspend (T) -> Boolean) =
    transform() { if (predicate(it)) it else null }

inline fun <T : Any, R : Any> Flow<T>.fusedMap(
    crossinline transform: suspend (T) -> R
) = transform { transform(it) }


private val source = flow {
    repeat(10_000) {
        emit(it)
    }
}.map { it + 1 }
    .map { it - 1 }
    .filter { true }


private val fusedInlineSource = flow {
    repeat(10_000) {
        emit(it)
    }
}.fusedMap() { it + 1 }
    .fusedMap { it - 1 }
    .fusedFilter() { true }

suspend fun main() {
    Flux.range(0, 10_000).map { it + 1 }
        .map { it - 1 }
        .filter() { true }
        .blockLast()
}