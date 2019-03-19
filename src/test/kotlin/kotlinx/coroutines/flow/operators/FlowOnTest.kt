package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FlowOnTest : TestBase() {

    private val captured = ArrayList<String>()

    @Test
    fun testFlowOn() = runTest("main") {
        val source = Source(42)
        val consumer = Consumer(42)

        val flow = source::produce.asFlow()
        flow.flowOn(named("ctx1")).launchIn(this) {
            onEach { consumer.consume(it)}
        }.join()

        assertEquals("ctx1", source.contextName)
        assertEquals("main", consumer.contextName)

        flow.flowOn(named("ctx2")).launchIn(this) {
            onEach { consumer.consume(it) }
        }.join()

        assertEquals("ctx2", source.contextName)
        assertEquals("main", consumer.contextName)
    }

    @Test
    fun testFlowOnAndOperators() = runTest("main") {
        val source = Source(42)
        val consumer = Consumer(42)
        val captured = ArrayList<String>()
        val mapper: suspend (Int) -> Int = {
            captured += captureName()
            it
        }

        val flow = source::produce.asFlow()
        flow.map(mapper)
            .flowOn(named("ctx1"))
            .map(mapper)
            .flowOn(named("ctx2"))
            .map(mapper)
            .launchIn(this) {
                onEach { consumer.consume(it)}
            }.join()

        assertEquals(listOf("ctx1", "ctx2", "main"), captured)
        assertEquals("ctx1", source.contextName)
        assertEquals("main", consumer.contextName)
    }

    @Test
    public fun testFlowOnThrowingSource() = runTest("main") {
        val flow = flow {
            emit(captureName())
            throw TestException()
        }.map { it }.flowOn(named("throwing"))

        assertFailsWith<TestException> { flow.single() }
        assertFailsWith<TestException>(flow)
        ensureActive()
    }

    @Test
    public fun testFlowOnThrowingOperator() = runTest("main") {
        val flow = flow {
            emit(captureName())
            delay(Long.MAX_VALUE)
        }.map {
            throw TestException(); it
        }.flowOn(named("throwing"))

        assertFailsWith<TestException> { flow.single() }
        assertFailsWith<TestException>(flow)
        ensureActive()
    }

    @Test
    public fun testFlowOnDownstreamOperator() = runTest("main") {
        val flow = flow {
            emit(captureName())
            delay(Long.MAX_VALUE)
        }.map {
            it
        }.flowOn(named("throwing"))
            .map { throw TestException() }

        assertFailsWith<TestException> { flow.single() }
        assertFailsWith<TestException>(flow)
        ensureActive()
    }

    @Test
    public fun testFlowOnThrowingConsumer() = runTest("main") {
        val flow = flow {
            emit(captureName())
            delay(Long.MAX_VALUE)
        }

        var success = false
        flow.flowOn(named("...")).launchIn(this + Dispatchers.Unconfined) {
            onEach { throw TestException() }
            catch<Throwable> { success = it is TestException }
        }.join()

        assertTrue(success)
        ensureActive()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFlowOnWithJob() = runTest("main") {
        flow {
            emit(1)
        }.flowOn(named("foo") + Job())
    }

    @Test
    fun testFlowOnCancellation() = runTest("main") {
        val captured = ArrayList<String>()
        val latch = Channel<Unit>()
        val job = launch {
            flow<Int> {
                latch.send(Unit)
                hang { captured += captureName() }
            }.flowOn(named("cancelled")).single()
        }

        latch.receive()
        job.cancel()
        job.join()
        assertEquals(listOf("cancelled"), captured)
        ensureActive()
    }

    @Test
    fun testFlowOnCancellationHappensBefore() = runTest("main") {
        val order = ArrayList<Int>()
        launch {
            try {
                // TODO expect
                flow<Int> {
                    order.add(1)
                    val flowJob = kotlin.coroutines.coroutineContext[Job]!!
                    launch {
                        order.add(2)
                        flowJob.cancel()
                    }
                    hang { order.add(3) }
                }.flowOn(named("upstream")).single()
            } catch (e: CancellationException) {
                order.add(4)
                assertEquals(listOf(1, 2, 3, 4), order)
            }
        }.join()
        ensureActive()
    }

    @Test
    fun testIndependentOperatorContext() = runTest("main") {
        val value = flow {
            captured += captureName()
            emit(-239)
        }.map {
            captured += captureName()
            it
        }.flowOn(named("base"))
            .map {
                captured += captureName()
                it
            }.single()

        assertEquals(-239, value)
        assertEquals(listOf("base", "base", "main"), captured)
    }

    @Test
    fun testMultipleFlowOn() = runTest("main") {
        flow {
            emit(1)
            captured += captureName()
        }.map { captured += captureName() }
            .flowOn(named("ctx1"))
            .map { captured += captureName() }
            .flowOn(named("ctx2"))
            .map { captured += captureName() }
            .flowOn(named("ctx3"))
            .map { captured += captureName() }
            .single()
        assertEquals(listOf("ctx1", "ctx1", "ctx2", "ctx3", "main"), captured)
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
