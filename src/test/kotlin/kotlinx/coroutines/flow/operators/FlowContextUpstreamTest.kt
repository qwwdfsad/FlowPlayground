package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FlowContextUpstreamTest : TestBase() {

    private val captured = ArrayList<String>()

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

        flow.withUpstreamContext(named("ctx2")).consumeOn(coroutineContext) {
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
            .consumeOn(coroutineContext) {
                consumer.consume(it)
            }.join()

        assertEquals(listOf("ctx1", "ctx1", "ctx1"), captured)
        assertEquals("ctx1", source.contextName)
        assertEquals("main", consumer.contextName)
    }

    @Test
    fun testFlowBuilderWithUpstream() = runTest {
        val result = flow(named("upstream")) { emit(captureName()) }
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
            emit(captureName())
            throw TestException()
        }

        assertFailsWith<TestException> { flow.map { it }.withUpstreamContext(named("throwing")).awaitSingle() }
    }

    @Test
    public fun testWithUpstreamThrowingOperator() = runTest {
        val flow = flow {
            emit(captureName())
            delay(Long.MAX_VALUE)
        }.map {
            throw TestException(); it
        }.withUpstreamContext(named("throwing"))

        assertFailsWith<TestException> { flow.awaitSingle() }
        assertFailsWith<TestException>(flow)
    }

    @Test
    public fun testWithUpstreamThrowingDownstreamOperator() = runTest {
        val flow = flow {
            emit(captureName())
            delay(Long.MAX_VALUE)
        }.map {
            it
        }.withUpstreamContext(named("throwing"))
            .map { throw TestException() }

        assertFailsWith<TestException> { flow.awaitSingle() }
        assertFailsWith<TestException>(flow)
    }

    @Test
    public fun testWithUpstreamThrowingConsumer() = runTest {
        val flow = flow {
            emit(captureName())
            delay(Long.MAX_VALUE)
        }

        var success = false
        flow.withUpstreamContext(named("...")).consumeOn(Dispatchers.Unconfined, onError = { success = it is TestException }) {
            throw TestException()
        }.join()

        assertTrue(success)
    }

    @Test(expected = IllegalStateException::class)
    fun testContextWithJob() = runTest {
        flow(named("foo") + Job()) {
            emit(1)
        }
    }

    @Test
    fun testUpstreamCancellation() = runTest {
        val captured = ArrayList<String>()
        val latch = Channel<Unit>()
        val job = launch {
            flow<Int>(named("cancelled")) {
                try {
                    latch.send(Unit)
                    delay(Long.MAX_VALUE)
                } finally {
                    captured += captureName()
                }
            }.awaitSingle()
        }

        latch.receive()
        job.cancel()
        job.join()
        assertEquals(listOf("cancelled"), captured)
    }

    @Test
    fun testUpstreamCancellationHappensBefore() = runTest {
        val order = ArrayList<Int>()
        launch {
            try {
                flow<Int>(named("upstream")) {
                    try {
                        order.add(1)
                        val flowJob = kotlin.coroutines.coroutineContext[Job]!!
                        launch {
                            order.add(2)
                            flowJob.cancel()
                        }
                        delay(Long.MAX_VALUE)
                    } finally {
                        order.add(3)
                    }
                }.awaitSingle()
            } catch (e: CancellationException) {
                order.add(4)
                assertEquals(listOf(1, 2, 3, 4), order)
            }
        }.join()
    }

    @Test
    fun testIndependentOperatorContext() = runTest {
        val value = flow {
            captured += captureName()
            emit(-239)
        }.map {
            captured += captureName()
            it
        }.withUpstreamContext(named("base"))
            .map {
                captured += captureName()
                it
            }.awaitSingle()

        assertEquals(-239, value)
        assertEquals(listOf("base", "base", "base"), captured)
    }

    @Test
    fun testMultipleUpstreamContexts() = runTest {
        flow {
            emit(1)
            captured += captureName()
        }.map { captured += captureName() }
            .withUpstreamContext(named("expected"))
            .map { captured += captureName() }
            .withUpstreamContext(named("ignored"))
            .map { captured += captureName() }
            .withUpstreamContext(named("ignored2"))
            .map { captured += captureName() }
            .awaitSingle()
        assertEquals(List(5) { "expected" }, captured)
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
