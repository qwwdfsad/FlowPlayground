package examples

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import java.util.*
import kotlin.system.*

/**
 * Example of how to write your own terminal operators
 */
suspend fun <T : Any> Flow<T>.first(): T {
    var result: T? = null
    return try {
        collect { value ->
            result = value
            throw FlowAbortedException()
        }
        result ?: throw NoSuchElementException("Flow was empty")
    } catch (e: FlowAbortedException) {
        result ?: error("Flow was empty")
    }
}

suspend fun <T : Any> Flow<T>.last(): T {
    var lastValue: T? = null
    collect { value ->
        lastValue = value
    }

    if (lastValue == null) throw NoSuchElementException("Flow was empty")
    return lastValue!!
}

suspend fun Flow<Int>.sum() = fold(0) { acc, value -> acc + value }

suspend fun main() {
    val flow = flow {
        emit(1)
        delay(1000)
        // Note that this one will not be printed for first
        println("Emitting after delay")
        emit(2)
    }

    println("'first' example:")
    val ms = measureTimeMillis { flow.first() }
    println("First element in $ms ms\n\n")

    println("'last' example:")
    val ms2 = measureTimeMillis { flow.last() }
    println("Last element in $ms2 ms")
}