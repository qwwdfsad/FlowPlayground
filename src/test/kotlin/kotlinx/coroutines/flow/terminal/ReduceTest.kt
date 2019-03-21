package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import org.junit.*
import org.junit.Assert.*

class ReduceTest : TestBase() {
    @Test
    fun testReduce() = runTest {
        val flow = flow {
            emit(1)
            emit(2)
        }

        val result = flow.reduce { value, acc -> value + acc }
        assertEquals(3, result)
    }

    @Test
    fun testEmptyReduce() = runTest {
        val flow = flowOf<Int>()
        assertFailsWith<UnsupportedOperationException> { flow.reduce { value, acc -> value + acc } }
    }

    @Test
    fun testErrorCancelsUpstream() = runTest {
        var cancelled = false
        val latch = Channel<Unit>()
        val flow = flow {
            coroutineScope {
                launch {
                    latch.send(Unit)
                    hang { cancelled = true }
                }
                emit(1)
                emit(2)
            }
        }

        assertFailsWith<TestException> {
            flow.reduce { _, _ ->
                latch.receive()
                throw TestException()
            }
        }
        assertTrue(cancelled)
    }
}
