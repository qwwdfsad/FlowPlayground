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
    val job = future.flow().launchIn(CoroutineScope(Dispatchers.Unconfined)) {
        onEach {
            println("Received $it")
        }
        finally {
            println("Done")
        }
    }

    println("Launched flow consumption, but future is not yet completed")
    Thread.sleep(500)
    future.complete(42)
    Thread.sleep(500)
    job.join()
}