package examples

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

fun eliminateTailCall() {

}

fun generate(): Flow<Int> {
    return flow {
        push(1)
        coroutineScope {
            launch {
                doAsyncPush()
                eliminateTailCall()
            }
        }
        eliminateTailCall()
    }
}

private fun doAsyncPush() {
    error("Exception with a weird stacktrace")
}

private suspend fun throwingProducer() {
    generate().map { it }
        .withUpstreamContext(newSingleThreadContext("upstream ctx 1"))
        .map { it }
        .withUpstreamContext(newSingleThreadContext("upstream ctx 2"))
        .consumeOn(newSingleThreadContext("downstream ctx 1"), onError = { it.printStackTrace() }) {
            println("You will see me once")
        }.join()
}

private suspend fun throwingOperator() {
    generate().map { it }
        .withUpstreamContext(newSingleThreadContext("upstream ctx 1"))
        .map { error("foo"); it }
        .withUpstreamContext(newSingleThreadContext("upstream ctx 2"))
        .consumeOn(newSingleThreadContext("downstream ctx 1"), onError = { it.printStackTrace() }) {
            println("You will never see me")
        }.join()
}

suspend fun main() {
    // Run with -ea
    throwingProducer()
    System.err.println("\n\n")
    throwingOperator()
}
