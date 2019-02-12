package kotlinx.coroutines.flow

import kotlinx.coroutines.*
import org.junit.*
import kotlin.test.*

open class TestBase {
    private val contexts = ArrayList<ExecutorCoroutineDispatcher>()

    @After
    fun tearDown() {
        contexts.forEach { it.close() }
        contexts.clear()
    }

    protected fun runTest(block: suspend CoroutineScope.() -> Unit) = runBlocking(named("main"), block)

    protected suspend inline fun <reified T> assertFailsWith(block: suspend () -> Unit) {
        try {
            block()
            error("Should not be reached")
        } catch (e: Throwable) {
            assertTrue(e is T)
        }
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