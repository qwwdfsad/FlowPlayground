package flow.operators

import flow.*

fun <T> Flow<T>.count(): Flow<Long> = flow {
    var i = 0L
    flowBridge {
        ++i
    }
    push(i)
}

suspend fun main() {
    println("Example 1: ${ runCatching { example1() }}")
    println("Example 2: ${ runCatching { example2() }}")
}

private suspend fun example2() {
    val f1 = flow(1).delay(5000).map { println("Whoa"); it }
    val f2 = flow(2).delay(100).map {
        error(":(")
        42
    }

    flow(f1, f2).flatMap { it }.count().flowBridge {
        println("$it elements")
    }
}


private suspend fun example1() {
    flow(1, 2, 3, 4).delayEach(100).count().flowBridge {
        println("$it elements")
    }
}