package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.coroutines.*
import kotlin.test.*

class FlowContextUpstreamTest : TestBase() {
    @Test
    fun testUpstreamContext() = runTest {
        val source = Source(42)
        val consumer = Consumer(42)

        val flow = source::produce.flow()
        flow.withUpstreamContext(named("ctx1")).consumeOn(coroutineContext) {
            consumer.consume(it)
        }.join()

        assertEquals("ctx1", source.contextName)
        assertEquals("main", consumer.contextName)

        flow.withUpstreamContext(named("ctx2")).consumeOn(coroutineContext[ContinuationInterceptor]!!) {
            consumer.consume(it)
        }.join()

        assertEquals("ctx2", source.contextName)
        assertEquals("main", consumer.contextName)
    }

    @Test
    fun testUpstreamContextAndOperators() = runTest {
        val source = Source(42)
        val consumer = Consumer(42)
        val captured = ArrayList<String>()
        val mapper: suspend (Int) -> Int = {
            captured += captureName()
            it
        }

        val flow = source::produce.flow()
        flow.map(mapper)
            .withUpstreamContext(named("ctx1"))
            .map(mapper)
            .withUpstreamContext(named("ctx2"))
            .map(mapper)
            .consumeOn(coroutineContext[ContinuationInterceptor]!!) {
                consumer.consume(it)
            }.join()

        assertEquals(listOf("ctx1", "ctx1", "ctx1"), captured)
        assertEquals("ctx1", source.contextName)
        assertEquals("main", consumer.contextName)
    }

    @Test
    fun testFlowBuilderWithUpstream() = runTest {
        val result = flow(named("upstream")) { push(captureName()) }
            .map {
                assertEquals("upstream", it)
                assertEquals("upstream", captureName())
                it
            }.withUpstreamContext(named("bar"))
            .map {
                assertEquals("upstream", it)
                assertEquals("upstream", captureName())
                it
            }.awaitSingle()

        assertEquals("upstream", result)
    }

    @Test
    public fun testWithUpstreamThrowingSource() = runTest {
        val flow = flow {
            push(captureName())
            throw TestException()
        }

        assertFailsWith<TestException> { flow.map { it }.withUpstreamContext(named("throwing")).awaitSingle() }
    }

    @Test
    public fun testWithUpstreamThrowingOperator() = runTest {
        val flow = flow {
            push(captureName())
            delay(Long.MAX_VALUE)
        }

        assertFailsWith<TestException> {
            flow
                .map { throw TestException(); it }
                .withUpstreamContext(named("throwing"))
                .awaitSingle()
        }
    }

    @Test
    public fun testWithUpstreamThrowingDownstreamOperator() = runTest {
        val flow = flow {
            push(captureName())
            delay(Long.MAX_VALUE)
        }

        assertFailsWith<TestException> {
            flow
                .map { it }
                .withUpstreamContext(named("throwing"))
                .map { throw TestException() }
                .awaitSingle()
        }
    }

    @Test
    public fun testWithUpstreamThrowingConsumer() = runTest {
        val flow = flow {
            push(captureName())
            delay(Long.MAX_VALUE)
        }

        var success = false
        flow.withUpstreamContext(named("...")).consumeOn(Dispatchers.Unconfined, onError = { success = it is TestException }) {
            throw TestException()
        }.join()

        assertTrue(success)
    }

    private inner class Source(private val value: Int) {
        public var contextName: String = "unknown"

        fun produce(): Int {
            contextName = captureName()
            return value
        }
    }

    private inner class Consumer(private val expected: Int) {
        public var contextName: String = "unknown"

        fun consume(value: Int) {
            contextName = captureName()
            assertEquals(expected, value)
        }
    }
}