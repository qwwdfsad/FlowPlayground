package kotlinx.coroutines.flow.sink

import examples.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import java.util.concurrent.*
import kotlin.concurrent.*
import kotlin.test.*

class FlowSinkTest {

    @Test
    fun testCompletableFuture() = runBlocking {
        val future = CompletableFuture<Int>()

        var element = -1
        var isDone = false
        val job = future.flow().consumeOn(Dispatchers.Unconfined, onComplete = { isDone = true }) {
            element = it
        }

        future.complete(42)
        job.join()
        assertTrue(isDone)
        assertEquals(42, element)
    }

    private class CallbackApi(val block: (FlowSink<Int>) -> Unit) {
        var started = false
        @Volatile
        var stopped = false
        lateinit var thread: Thread

        fun start(sink: FlowSink<Int>) {
            started = true
            thread = thread {
                while (!stopped) {
                    block(sink)
                }
            }
        }

        fun stop() {
            stopped = true
        }
    }

    @Test
    fun testThrowingConsumer() = runBlocking {
        var i = 0
        val api = CallbackApi {
            it.offer(++i)
        }


        val flow = flowViaSink<Int> { sink ->
            api.start(sink)
            try {
                sink.join()
            } finally {
                api.stop()
            }
        }

        var receivedConsensus = 0
        var isDone = false
        var exception: Throwable? = null
        val job = flow
            .filter { it > 10 }
            .consumeOn(Dispatchers.Unconfined, onComplete = { isDone = true }, onError = { exception = it }) {
                if (it == 11) {
                    ++receivedConsensus
                } else {
                    receivedConsensus = 42
                }
                throw RuntimeException()
            }

        job.join()
        assertEquals(1, receivedConsensus)
        assertFalse(isDone)
        assertTrue { exception is java.lang.RuntimeException }

        assertTrue(api.started)
        assertTrue(api.stopped)
        api.thread.join()
    }

    @Test
    fun testThrowingSource() = runBlocking {
        var i = 0
        val api = CallbackApi {
            it.offer(++i)
            if (i == 5) it.completeExceptionally(java.lang.RuntimeException())
        }

        val flow = flowViaSink<Int> { sink ->
            api.start(sink)
            sink.join()
            api.stop()
        }

        var received = 0
        var isDone = false
        var exception: Throwable? = null
        val job = flow
            .consumeOn(Dispatchers.Unconfined, onComplete = { isDone = true }, onError = { exception = it }) {
                ++received
            }

        job.join()
        assertFalse(isDone)
        assertTrue { exception is java.lang.RuntimeException }

        assertTrue(api.started)
        assertTrue(api.stopped)
        api.thread.join()
    }
}
