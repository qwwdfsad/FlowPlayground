package kotlinx.coroutines.flow.operators

import examples.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class DropWhileTest : TestBase() {
    @Test
    fun testDropWhile() = runTest {
        val flow = flow {
            emit(1)
            emit(2)
            emit(3)
        }

        assertEquals(6, flow.dropWhile { false }.sum())
        assertNull(flow.dropWhile { true }.singleOrNull())
        assertEquals(5, flow.dropWhile { it < 2 }.sum())
        assertEquals(1, flow.take(1).dropWhile { it > 1 }.single())
    }

    @Test
    fun testEmptyFlow() = runTest {
        assertEquals(0, flowOf<Int>().dropWhile { true }.sum())
        assertEquals(0, flowOf<Int>().dropWhile { false }.sum())
    }

    @Test
    fun testErrorCancelsUpstream() = runTest {
        var cancelled = false
        val flow = flow {
            coroutineScope {
                launch(start = CoroutineStart.ATOMIC) {
                    hang { cancelled = true }
                }
                emit(1)
            }
        }.dropWhile {
            throw TestException()
        }

        assertFailsWith<TestException>(flow)
        assertTrue(cancelled)
        assertEquals(42, flow.onErrorReturn(42).single())
        assertEquals(42, flow.onErrorCollect(flowOf(42)).single())
    }
}
