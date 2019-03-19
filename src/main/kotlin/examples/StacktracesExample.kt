package examples

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.terminal.*

/**
 * Simple example that shows how exceptions are printed with debug mode enabled
 */
fun generate(): Flow<Int> {
    return flow {
        emit(1)
        coroutineScope {
            launch {
                doAsyncPush()
                eliminateTailCall()
            }
        }
        eliminateTailCall()
    }
}
fun eliminateTailCall() {}

fun doAsyncPush() {
    error("Exception with a weird stacktrace")
}

private suspend fun throwingProducer() {
    generate().map { it }
        .flowOn(newSingleThreadContext("upstream ctx 1"))
        .map { it }
        .flowOn(newSingleThreadContext("upstream ctx 2"))
        .launchIn(GlobalScope + newSingleThreadContext("downstream ctx 1")) {
            onEach { println("You will see me once") }
            catch<Throwable> {
                it.printStackTrace()
            }
        }.join()
}

private suspend fun throwingOperator() {
    generate().map { it }
        .flowOn(newSingleThreadContext("upstream ctx 1"))
        .map { error("foo") }
        .flowOn(newSingleThreadContext("upstream ctx 2"))
        .launchIn(GlobalScope + newSingleThreadContext("downstream ctx 1")) {
            onEach { println("You will never see me") }
            catch<Throwable> {
                it.printStackTrace()
            }
        }.join()
}

suspend fun main() {
    // TODO KT-XXXXX :(
    // Run with -ea in order to enable debug mode, (add inline to flow and collect methods)
    throwingProducer()
    System.err.println("\n\n")
    throwingOperator()
}
