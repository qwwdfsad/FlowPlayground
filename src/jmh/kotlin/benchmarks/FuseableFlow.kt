package benchmarks

import examples.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.terminal.*

typealias Transformer<T, R> = suspend (T) -> R?

// TODO it could be non-public
abstract class FuseableFlow<T : Any, R : Any>(private val flow: Flow<T>) : Flow<R> {

    public val sourceFlow: Flow<*> = nonFuseableFlow(this)

    private tailrec fun nonFuseableFlow(flow: Flow<*>): Flow<*> {
        if (flow !is FuseableFlow<*, *>) {
            return flow
        }

        return nonFuseableFlow(flow.flow)
    }

    protected abstract suspend fun transform(value: T): R?

    public suspend fun transformChained(value: Any): R? {
        if (flow is FuseableFlow<*, *>) {
            val tr = flow.transformChained(value) ?: return null
            return transform(tr as T)
        } else {
            return transform(value as T)
        }
    }
}

inline fun <T : Any, R : Any> Flow<T>.transform(crossinline transformer: Transformer<T, R>): Flow<R> =
    object : FuseableFlow<T, R>(this) {
        override suspend fun transform(value: T): R? = transformer(value)

        override suspend fun collect(collector: FlowCollector<R>)=
            sourceFlow.collect {
                val value = transformChained(it)
                if (value != null) collector.emit(value)
            }
    }

inline fun <T : Any> Flow<T>.fusedFilter(crossinline predicate: suspend (T) -> Boolean) =
    transform { if (predicate(it)) it else null }


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
    repeat(1) {
        emit(it)
    }
}.fusedMap() { it + 1 }
    .fusedMap { it - 1 }
    .fusedFilter() { true }

suspend fun main() {
    println(fusedInlineSource.sum())
}