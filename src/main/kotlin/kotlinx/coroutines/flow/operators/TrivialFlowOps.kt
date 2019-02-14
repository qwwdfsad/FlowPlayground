@file:Suppress("UNCHECKED_CAST")

package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*

// TODO this one should be inline for performance after all crossinline fixes and tests coverage
//@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
//@kotlin.internal.InlineOnly
suspend inline fun <T : Any> Flow<T>.collect(crossinline action: suspend (T) -> Unit): Unit =
    collect(object : FlowCollector<T> {
        override suspend fun emit(value: T) = action(value)
    })

fun <T : Any> Flow<T>.filter(predicate: suspend (T) -> Boolean): Flow<T> =
    flow {
        collect { value ->
            if (predicate(value)) emit(value)
        }
    }

fun <T : Any, R : Any> Flow<T>.map(transform: suspend (T) -> R): Flow<R> =
    flow {
        collect { value ->
            emit(transform(value))
        }
    }

fun <T : Any> Flow<T>.delayFlow(millis: Long): Flow<T> =
    flow {
        kotlinx.coroutines.delay(millis)
        collect {
            emit(it)
        }
    }

fun <T : Any> Flow<T>.delayEach(millis: Long): Flow<T> =
    flow {
        collect {
            kotlinx.coroutines.delay(millis)
            emit(it)
        }
    }

fun <T : Any> Flow<T>.distinctUntilChanged(): Flow<T> =
    flow {
        var previous: T? = null
        collect {
            if (previous != it) {
                previous = it
                emit(it)
            }
        }
    }