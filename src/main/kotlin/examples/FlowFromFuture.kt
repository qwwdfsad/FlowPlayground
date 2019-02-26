package examples

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import java.util.concurrent.*

// Though we do not recommend to do it :)
fun <T : Any> CompletableFuture<T>.flow(): Flow<T> = flowViaChannel { sink ->
    whenComplete { element, error ->
        if (error != null) {
            sink.close(error)
        } else {
            require(sink.offer(element)) // Should always succeed
            sink.close()
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