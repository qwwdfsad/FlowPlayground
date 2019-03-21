package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import org.junit.*
import org.junit.Test
import kotlin.test.*

class FoldTest : TestBase() {
    @Test
    fun testFold() = runTest {
        val flow = flow {
            emit(1)
            emit(2)
        }

        val result = flow.fold(3) { value, acc -> value + acc }
        assertEquals(6, result)
    }

    @Test
    fun testEmptyFold() = runTest {
        val flow = flowOf<Int>()
        assertEquals(42, flow.fold(42) { value, acc -> value + acc })
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
            }
        }

        assertFailsWith<TestException> {
            flow.fold(42) { _, _ ->
                latch.receive()
                throw TestException()
            }
        }
        Assert.assertTrue(cancelled)
    }
}