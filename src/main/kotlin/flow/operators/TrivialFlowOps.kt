@file:Suppress("UNCHECKED_CAST")

package flow.operators

import flow.*

// TODO this one should be inline for performance
suspend fun <T> Flow<T>.flowBridge(action: suspend (T) -> Unit): Unit =
    subscribe(object : FlowSubscriber<T> {
        override suspend fun push(value: T) = action(value)
    })


inline fun <T> Flow<T>.filter(crossinline predicate: suspend (T) -> Boolean): Flow<T> = flow {
    flowBridge { value ->
        if (predicate(value)) push(value)
    }
}

inline fun <T, R> Flow<T>.map(crossinline transform: suspend (T) -> R): Flow<R> = flow {
    flowBridge { value ->
        push(transform(value))
    }
}

fun <T> Flow<T>.delay(millis: Long): Flow<T> = flow {
    kotlinx.coroutines.delay(millis)
    flowBridge {
        push(it)
    }
}

fun <T> Flow<T>.delayEach(millis: Long): Flow<T> = flow {
    flowBridge {
        kotlinx.coroutines.delay(millis)
        push(it)
    }
}

fun <T> Flow<T>.distinctUntilChanged(): Flow<T> = flow {
    var previous: T? = null
    flowBridge {
        if (previous != it) {
            previous = it
            push(it)
        }
    }
}

suspend fun main()  {
    listOf(1, 1, 2, 1, 3).asFlow().delay(3000).distinctUntilChanged().flowBridge {
        println("HM: $it")
    }
}