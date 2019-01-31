@file:Suppress("UNCHECKED_CAST")

package flow

suspend fun <T> Flow<T>.consumeEach(action: suspend (T) -> Unit) =
    consumeEach(object : FlowSubscription<T> {
        override suspend fun push(value: T) = action(value)
    })


inline fun <T> Flow<T>.filter(crossinline predicate: suspend (T) -> Boolean): Flow<T> = flow {
    consumeEach { value ->
        if (predicate(value)) push(value)
    }
}

inline fun <T, R> Flow<T>.map(crossinline transform: suspend (T) -> R): Flow<R> = flow {
    consumeEach { value ->
        push(transform(value))
    }
}

fun <T> Flow<T>.delay(millis: Long): Flow<T> = flow {
    kotlinx.coroutines.delay(millis)
    consumeEach {
        push(it)
    }
}

fun <T> Flow<T>.delayEach(millis: Long): Flow<T> = flow {
    consumeEach {
        kotlinx.coroutines.delay(millis)
        push(it)
    }
}

fun <T> Flow<T>.distinctUntilChanged(): Flow<T> = flow {
    var previous: T? = null
    consumeEach {
        if (previous != it) {
            previous = it
            push(it)
        }
    }
}

suspend fun main()  {
    listOf(1, 1, 2, 1, 3).asFlow().delay(3000).distinctUntilChanged().consumeEach {
        println("HM: $it")
    }
}