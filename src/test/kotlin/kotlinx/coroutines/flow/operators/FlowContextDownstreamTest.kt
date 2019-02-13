package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FlowContextDownstreamTest : TestBase() {

    private val captured = ArrayList<String>()

    @Test
    fun testWithDownstreamContext() = runTest {
        val captured = ArrayList<String>()
        val flow = flow {
            captured += captureName()
            push(314)
        }

        val mapper: suspend (Int) -> Int = {
            captured += captureName()
            it
        }

        val result = flow.withDownstreamContext(named("ctx1"))
            .map(mapper)
            .withDownstreamContext(named("ctx2"))
            .map(mapper)
            .awaitSingle()

        assertEquals(314, result)
        assertEquals(listOf("main", "ctx1", "ctx2"), captured)
    }

    @Test
    public fun testWithDownstreamThrowingSource() = runTest {
        val flow = flow {
            push(captureName())
            throw TestException()
        }.map { it }.withDownstreamContext(named("throwing"))

        assertFailsWith<TestException> { flow.awaitSingle() }
        assertFailsWith<TestException>(flow)
    }

    @Test
    public fun testWithDownstreamThrowingOperator() = runTest {
        val flow = flow {
            push(captureName())
            delay(Long.MAX_VALUE)
        }.map {
            throw TestException();
            it
        }.withDownstreamContext(named("throwing"))

        assertFailsWith<TestException> { flow.awaitSingle() }
        assertFailsWith<TestException>(flow)
    }

    @Test
    public fun testWithDownstreamThrowingDownstreamOperator() = runTest {
        val flow = flow {
            push(captureName())
            delay(Long.MAX_VALUE)
        }.map { it }
            .withDownstreamContext(named("throwing"))
            .map { throw TestException() }

        assertFailsWith<TestException> { flow.awaitSingle() }
        assertFailsWith<TestException>(flow)
    }

    @Test
    fun testMultipleDownstreamContexts() = runTest() {
        val mapper: suspend (Unit) -> Unit = {
            captured += captureName()
        }

        flow {
            captured += captureName()
            push(Unit)
        }.map(mapper)
            .withDownstreamContext(named("downstream"))
            .map(mapper)
            .withDownstreamContext(named("downstream2"))
            .map(mapper)
            .withDownstreamContext(named("downstream3"))
            .map(mapper)
            .withDownstreamContext(named("ignored"))
            .awaitSingle()

        assertEquals(listOf("main", "main", "downstream", "downstream2", "downstream3"), captured)
    }

    @Test
    fun testDownstreamCancellation() = runTest {
        val captured = ArrayList<String>()
        val latch = Channel<Unit>()
        val job = launch {
            flow<Int> {
                try {
                    latch.send(Unit)
                    delay(Long.MAX_VALUE)
                } finally {
                    captured += captureName()
                }
            }
                .withDownstreamContext(named("cancelled"))
                .awaitSingle()
        }

        latch.receive()
        job.cancel()
        job.join()
        assertEquals(listOf("main"), captured)
    }

    @Test
    fun testDownstreamCancellationHappensBefore() = runTest {
        val order = ArrayList<Int>()
        launch {
            try {
                flow<Int> {
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
                }.withDownstreamContext(named("downstream")).awaitSingle()
            } catch (e: CancellationException) {
                order.add(4)
                assertEquals(listOf(1, 2, 3, 4), order)
            }
        }.join()
    }

    @Test
    fun testMultipleDownstreamContextsWithJobs() = runTest() {
        var switch = 0
        val flow = flow {
            captured += captureName()
            push(Unit)
            if (switch == 0) throw TestException()
        }.map { if (switch == 1) throw TestException() else it }
            .withDownstreamContext(named("downstream") + Job())
            .map { if (switch == 2) throw TestException() else it }
            .withDownstreamContext(named("downstream2") + Job())

        repeat(3) {
            switch = it
            assertFailsWith<TestException> { flow.awaitSingle() }
            assertFailsWith<TestException>(flow)
        }
    }

    @Test
    fun testMultipleDownstreamContextsWithJobsCancellation() = runTest() {
        fail("Discuss it")
        val latch = Channel<Unit>()
        var invoked = false
        val flow = flow {
            captured += captureName()
            push(Unit)
            latch.receive()
            try {
                delay(Long.MAX_VALUE)
            } finally {
                invoked = true
            }
        }.map { it }.withDownstreamContext(named("downstream2") + Job())

        val job = launch {
            flow.awaitSingle()
        }
        latch.receive()
        job.cancelAndJoin()
        assertTrue(invoked)
    }
}
