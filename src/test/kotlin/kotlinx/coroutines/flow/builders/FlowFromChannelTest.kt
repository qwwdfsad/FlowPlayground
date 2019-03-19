package kotlinx.coroutines.flow.builders

import examples.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import java.util.concurrent.*
import kotlin.concurrent.*
import kotlin.test.*

class FlowFromChannelTest {

    @Test
    fun testCompletableFuture() = runBlocking {
        val future = CompletableFuture<Int>()
        var element = -1
        var isDone = false
        val job = future.flow().launchIn(this) {
            onEach { element = it }
            finally { isDone = true }
        }

        future.complete(42)
        job.join()
        assertTrue(isDone)
        assertEquals(42, element)
    }

    private class CallbackApi(val block: (SendChannel<Int>) -> Unit) {
        var started = false
        @Volatile
        var stopped = false
        lateinit var thread: Thread

        fun start(sink: SendChannel<Int>) {
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
            runCatching {  it.offer(++i) }
        }


        val flow = flowViaChannel<Int> { channel ->
            api.start(channel)
            channel.invokeOnClose {
                api.stop()
            }
        }

        var receivedConsensus = 0
        var isDone = false
        var exception: Throwable? = null
        val job = flow
            .filter { it > 10 }
            .launchIn(this) {
                onEach {
                    if (it == 11) {
                        ++receivedConsensus
                    } else {
                        receivedConsensus = 42
                    }
                    throw RuntimeException()
                }
                catch<Throwable> { exception = it }
                finally { isDone = true }
            }
        job.join()
        assertEquals(1, receivedConsensus)
        assertTrue(isDone)
        assertTrue { exception is RuntimeException }

        assertTrue(api.started)
        assertTrue(api.stopped)
        api.thread.join()
    }

    @Test
    fun testThrowingSource() = runBlocking {
        var i = 0
        val api = CallbackApi {
            it.offer(++i)
            if (i == 5) it.close(RuntimeException())
        }

        val flow = flowViaChannel<Int> { channel ->
            api.start(channel)
            channel.invokeOnClose {
                api.stop()
            }
        }

        var received = 0
        var isDone = false
        var exception: Throwable? = null
        val job = flow.launchIn(this) {
            onEach { ++received }
            catch<Throwable> { exception = it }
            finally { isDone = true }
        }

        job.join()
        assertTrue(isDone)
        assertTrue { exception is java.lang.RuntimeException }

        assertTrue(api.started)
        assertTrue(api.stopped)
        api.thread.join()
    }
}
