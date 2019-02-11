package examples

import flow.*
import flow.sink.*
import flow.terminal.*
import kotlinx.coroutines.*
import java.util.concurrent.*

fun <T : Any> CompletableFuture<T>.flow(): Flow<T> = FlowSink.create { sink ->
    whenComplete { element, error ->
        if (error != null) {
            sink.onException(error)
        } else {
            sink.onNext(element)
            sink.onCompleted()
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