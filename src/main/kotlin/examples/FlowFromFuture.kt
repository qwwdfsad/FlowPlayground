package examples

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.sink.*
import kotlinx.coroutines.flow.terminal.*
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