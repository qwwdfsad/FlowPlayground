package examples

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.sink.*
import kotlinx.coroutines.flow.terminal.*
import java.util.concurrent.*

// Though we do not recommend to do it :)
fun <T : Any> CompletableFuture<T>.flow(): Flow<T> = FlowSink.create { sink ->
    whenComplete { element, error ->
        if (error != null) {
            sink.error(error)
        } else {
            sink.next(element)
            sink.completed()
        }
    }
}

suspend fun main() {
    val future = CompletableFuture<Int>()
    val job = future.flow().consumeOn(Dispatchers.Unconfined, onComplete = { println("Done") }) {
        println("Received $it")
    }

    Thread.sleep(500)
    future.complete(42)
    Thread.sleep(500)
    job.join()
}