package kotlinx.coroutines.flow.operators

import examples.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class MapTest : TestBase() {

    @Test
    fun testMap() = runTest {
        val flow = flow {
            emit(1)
            emit(2)
        }

        val result = flow.map { it + 1 }.sum()
        assertEquals(5, result)
    }

    @Test
    fun testEmptyFlow() = runTest {
        val sum = flowOf<Int>().map { it + 1 }.sum()
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
                    hang { cancelled = true }
                }
                emit(1)
            }
        }.map {
            latch.receive()
            throw TestException()
            it + 1
        }.onErrorReturn(42)

        assertEquals(42, flow.single())
        assertTrue(cancelled)
    }
}
