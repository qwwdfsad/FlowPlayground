package kotlinx.coroutines.flow

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.*
import kotlin.coroutines.*
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

    protected inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit) {
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
        return when {
            name.contains("%%") -> name.substringAfter("%%").substringBefore("%%")
            name.startsWith("main ") -> "main"
            else -> name
        }
    }

    protected suspend fun ensureActive() {
        assertTrue(coroutineContext.isActive)
        assertFalse(coroutineContext[Job]!!.isCancelled)
    }

    protected suspend inline fun hang(onCancellation: () -> Unit) {
        try {
            suspendCancellableCoroutine<Unit> { }
        } finally {
            onCancellation()
        }

    }
}

class TestException : Throwable()
