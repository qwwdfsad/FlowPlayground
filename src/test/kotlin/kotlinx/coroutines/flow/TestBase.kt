package kotlinx.coroutines.flow

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.*
import kotlin.test.*

open class TestBase {
    private val contexts = ArrayList<ExecutorCoroutineDispatcher>()

    @After
    fun tearDown() {
        contexts.forEach { it.close() }
        contexts.clear()
    }

    protected fun runTest(block: suspend CoroutineScope.() -> Unit) = runBlocking(named("main")) {
        block()
        try {
            assert(isActive)
            val job = coroutineContext[Job]!!
            assertTrue(job.isActive)
            assertTrue(!job.isCancelled)
            yield()
        } catch (e: Throwable) {
            fail("Should not be cancelled")
        }
    }

    protected suspend inline fun <reified T : Throwable> assertFailsWith(block: suspend () -> Unit) {
        try {
            block()
            error("Should not be reached")
        } catch (e: Throwable) {
            assertTrue(e is T)
        }
    }

    protected suspend inline fun <reified T : Throwable> assertFailsWith(flow: Flow<*>) {
        var e: Throwable? = null
        var completed = false
        flow.consumeOn(Dispatchers.Unconfined, onError = { e = it }, onComplete = { completed = true }, onNext = {}).join()
        assertTrue(e is T)
        assertFalse(completed)
    }

    protected fun named(name: String) = newSingleThreadContext("%%$name%%").also { contexts.add(it) }

    protected fun captureName(): String {
        val name = Thread.currentThread().name
        return if (name.contains("%%")) name.substringAfter("%%").substringBefore("%%")
        else if (name.startsWith("main ")) "main"
        else name
    }
}

class TestException() : Throwable()