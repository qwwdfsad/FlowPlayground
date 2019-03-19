package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FlowWithTest : TestBase() {

    private val captured = ArrayList<String>()

    @Test
    fun testFlowWith() = runTest("main") {
        val captured = ArrayList<String>()
        val flow = flow {
            captured += captureName()
            emit(314)
        }

        val mapper: suspend (Int) -> Int = {
            captured += captureName()
            it
        }

        val result = flow.flowWith(named("ctx1")) {
            map(mapper)
        }.flowWith(named("ctx2")) {
            map(mapper)
        }.map(mapper)
            .single()

        assertEquals(314, result)
        assertEquals(listOf("main", "ctx1", "ctx2", "main"), captured)
    }

    @Test
    public fun testFlowWithThrowingSource() = runTest("main") {
        val flow = flow {
            emit(captureName())
            throw TestException()
        }.flowWith(named("throwing")) {
            map { it }
        }

        assertFailsWith<TestException> { flow.single() }
        assertFailsWith<TestException>(flow)
        ensureActive()
    }

    @Test
    public fun testFlowWithThrowingOperator() = runTest("main") {
        val flow = flow {
            emit(captureName())
            delay(Long.MAX_VALUE)
        }.flowWith(named("throwing")) {
            map {
                throw TestException()
            }
        }

        assertFailsWith<TestException> { flow.single() }
        assertFailsWith<TestException>(flow)
        ensureActive()
    }

    @Test
    public fun testFlowWithThrowingDownstreamOperator() = runTest("main") {
        val flow = flow {
            emit(captureName())
            delay(Long.MAX_VALUE)
        }.flowWith(named("throwing")) {
            map { it }
        }
            .map { throw TestException() }

        assertFailsWith<TestException> { flow.single() }
        assertFailsWith<TestException>(flow)
        ensureActive()
    }

    @Test
    fun testMultipleFlowWith() = runTest("main") {
        val mapper: suspend (Unit) -> Unit = {
            captured += captureName()
        }

        flow {
            captured += captureName()
            emit(Unit)
        }.map(mapper)
            .flowWith(named("downstream")) {
                map(mapper)
            }
            .flowWith(named("downstream 2")) {
                map(mapper)
            }
            .flowWith(named("downstream 3")) {
                map(mapper)
            }
            .map(mapper)
            .flowWith(named("downstream 4")) {
                map(mapper)
            }.flowWith(named("ignored")) { this }
            .single()

        assertEquals(
            listOf("main", "main", "downstream", "downstream 2", "downstream 3", "main", "downstream 4"),
            captured
        )
    }

    @Test
    fun testFlowWithCancellation() = runTest("main") {
        val captured = ArrayList<String>()
        val latch = Channel<Unit>()
        val job = launch {
            flow<Int> {
                latch.send(Unit)
                hang { captured += captureName() }
            }.flowWith(named("cancelled")) {
                map { it }
            }.single()
        }

        latch.receive()
        job.cancel()
        job.join()
        assertEquals(listOf("main"), captured)
        ensureActive()
    }

    @Test
    fun testFlowWithCancellationHappensBefore() = runTest("main") {
        val order = ArrayList<Int>()
        launch {
            try {
                flow<Int> {
                    order.add(1)
                    val flowJob = kotlin.coroutines.coroutineContext[Job]!!
                    launch {
                        order.add(2)
                        flowJob.cancel()
                    }
                    hang { order.add(3) }
                }.flowWith(named("downstream")) {
                    map { it }
                }
            } catch (e: CancellationException) {
                order.add(4)
                assertEquals(listOf(1, 2, 3, 4), order)
            }
        }.join()
    }

    @Test
    fun testMultipleFlowWithException() = runTest("main") {
        var switch = 0
        val flow = flow {
            captured += captureName()
            emit(Unit)
            if (switch == 0) throw TestException()
        }.map { if (switch == 1) throw TestException() else Unit }
            .flowWith(named("downstream")) {
                map { if (switch == 2) throw TestException() else Unit }
            }
        repeat(3) {
            switch = it
            assertFailsWith<TestException> { flow.single() }
            assertFailsWith<TestException>(flow)
        }
    }

    @Test(timeout = 1000L)
    fun testMultipleFlowWithJobsCancellation() = runTest("main") {
        val latch = Channel<Unit>()
        var invoked = false
        val flow = flow {
            captured += captureName()
            emit(Unit)
            latch.send(Unit)
            hang { invoked = true }
        }.flowWith(named("downstream")) {
            map { Unit }
        }

        val job = launch {
            flow.single()
        }

        latch.receive()
        job.cancelAndJoin()
        assertTrue(invoked)
        ensureActive()
    }
}
