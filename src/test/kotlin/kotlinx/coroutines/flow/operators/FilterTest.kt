package kotlinx.coroutines.flow.operators

import examples.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FilterTest : TestBase() {
    @Test
    fun testFilter() = runTest {
        val flow = flowOf(1, 2)
        assertEquals(2, flow.filter { it % 2 == 0 }.sum())
        assertEquals(3, flow.filter { true }.sum())
        assertEquals(0, flow.filter { false }.sum())
    }

    @Test
    fun testEmptyFlow() = runTest {
        val sum = flowOf<Int>().filter { true }.sum()
        assertEquals(0, sum)
    }

    @Test
    fun testErrorCancelsUpstream() = runTest {
        var cancelled = false
        val latch = Channel<Unit>()
        val flow = flow {
            coroutineScope {
                launch {
                    latch.send(Unit)
                    hang {cancelled = true}
                }
                emit(1)
            }
        }.filter {
            latch.receive()
            throw TestException()
            true
        }.onErrorReturn(42)

        assertEquals(42, flow.single())
        assertTrue(cancelled)
    }
}
