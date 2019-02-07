package flow.operators

import flow.*
import flow.source.*

fun <T> Flow<Flow<T>>.concat(): Flow<T> = flow {
    flowBridge {
        val inner = it
        inner.flowBridge { value ->
            push(value)
        }
    }
}

suspend fun main() {
    val f1 = flow(1, 2, 3).delayEach(1000)
    val f2 = flow(1, 2, 3).delay(1000).map { 3 + it }
    flow(f1, f2).concat().flowBridge {
        println(it)
    }
}