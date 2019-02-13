package examples

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.terminal.*
import java.util.*


suspend fun <T : Any> Flow<T>.first(): T {
    var result: T? = null
    try {
        flowBridge { value ->
            result = value
        }
        return result ?: throw NoSuchElementException("Flow is empty")
    } catch (e: FlowConsumerAborted) {
        return result!!
    }
}

suspend fun <T : Any> Flow<T>.last(): T {
    var lastValue: T? = null
    flowBridge { value ->
        lastValue = value
    }

    if (lastValue == null) throw NoSuchElementException("Flow is empty")
    return lastValue!!
}

suspend fun Flow<Int>.sum() = fold(0) { acc, value -> acc + value }


suspend fun main() {
    val flow = flow(Dispatchers.Default) {
        println("Computing sequence in CPU thread")
        repeat(Int.MAX_VALUE) {
            if (it > 4) println("Whoa, should not happen!")
            push(it)
        }
    }

    val sum = flow.limit(5).sum()
    println("Sum: $sum")
}