@file:Suppress("UNCHECKED_CAST")

package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*

// TODO this one should be inline for performance after all crossinline fixes and tests coverage
//@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
//@kotlin.internal.InlineOnly
suspend inline fun <T : Any> Flow<T>.flowBridge(crossinline action: suspend (T) -> Unit): Unit =
    subscribe(object : FlowSubscriber<T> {
        override suspend fun push(value: T) = action(value)
    })

fun <T : Any> Flow<T>.filter(predicate: suspend (T) -> Boolean): Flow<T> =
    flow {
        flowBridge { value ->
            if (predicate(value)) push(value)
        }
    }

fun <T : Any, R : Any> Flow<T>.map(transform: suspend (T) -> R): Flow<R> =
    flow {
        flowBridge { value ->
            push(transform(value))
        }
    }

fun <T : Any> Flow<T>.delay(millis: Long): Flow<T> =
    flow {
        kotlinx.coroutines.delay(millis)
        flowBridge {
            push(it)
        }
    }

fun <T : Any> Flow<T>.delayEach(millis: Long): Flow<T> =
    flow {
        flowBridge {
            kotlinx.coroutines.delay(millis)
            push(it)
        }
    }

fun <T : Any> Flow<T>.distinctUntilChanged(): Flow<T> =
    flow {
        var previous: T? = null
        flowBridge {
            if (previous != it) {
                previous = it
                push(it)
            }
        }
    }