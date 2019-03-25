package kotlinx.coroutines.flow.operators

import examples.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class TakeWhileTest : TestBase() {
    @Test
    fun testTakeWhile() = runTest {
        val flow = flow {
            emit(1)
            emit(2)
        }

        assertEquals(3, flow.takeWhile { true }.sum())
        assertEquals(1, flow.takeWhile { it < 2 }.single())
        assertEquals(2, flow.drop(1).takeWhile { it < 3 }.single())
        assertNull(flow.drop(1).takeWhile { it < 2 }.singleOrNull())
    }

    @Test
    fun testEmptyFlow() = runTest {
        assertEquals(0, flowOf<Int>().takeWhile { true }.sum())
        assertEquals(0, flowOf<Int>().takeWhile { false }.sum())
    }

    @Test
    fun testCancelUpstream() = runTest {
        var cancelled = false
        val flow = flow {
            coroutineScope {
                launch(start = CoroutineStart.ATOMIC) {
                    hang { cancelled = true }
                }

                emit(1)
                emit(2)
            }
        }

        assertEquals(1, flow.takeWhile { it < 2 }.single())
        assertTrue(cancelled)
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
        }.takeWhile {
            throw TestException()
        }

        assertFailsWith<TestException>(flow)
        assertTrue(cancelled)
        assertEquals(42, flow.onErrorReturn(42).single())
        assertEquals(42, flow.onErrorCollect(flowOf(42)).single())
    }
}
