package examples

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.operators.*
import kotlin.system.*

suspend fun <T : Any, R : Any> Flow<T>.fusedFilterMap(
    predicate: (T) -> Boolean,
    mapper: (T) -> R
): Flow<R> = flow {
    flowBridge { value ->
        if (predicate(value)) push(mapper(value))
    }
}

suspend fun Flow<Int>.delayEachEven(timeout: Long): Flow<Int> = flow {
    flowBridge { value ->
        if (value % 2 == 0) {
            delay(timeout)
        }
        push(value)
    }
}

suspend fun main() {
    val flow = flow {
        repeat(5) {
            push(it)
        }
    }
        .fusedFilterMap({ it > 2 }, { it + 1 }) // 4, 5
        .delayEachEven(1000)


    val time = measureTimeMillis {
        println("Sum ${flow.sum()}")
    }
    println("Computed in $time ms")
}