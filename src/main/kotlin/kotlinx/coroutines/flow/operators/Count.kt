package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*

fun <T: Any> Flow<T>.count(): Flow<Long> =
    flow {
        var i = 0L
        collect {
            ++i
        }
        emit(i)
    }

suspend fun main() {
    println("Example 1: ${ runCatching { throwingProducer() }}")
    println("Example 2: ${ runCatching { example2() }}")
}

private suspend fun example2() {
    val f1 = flowOf(1).delayFlow(5000).map { println("Whoa"); it }
    val f2 = flowOf(2).delayFlow(100).map {
        error(":(")
        42
    }

    flowOf(f1, f2).flatMap { it }.count().collect {
        println("$it elements")
    }
}


private suspend fun throwingProducer() {
    flowOf(1, 2, 3, 4).delayEach(100).count().collect {
        println("$it elements")
    }
}