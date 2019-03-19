package kotlinx.coroutines.flow.operators

import examples.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import org.junit.Test
import kotlin.test.*

class DistinctUntilChangedTest : TestBase() {

    private class Box(val i: Int)

    @Test
    fun testDistinctUntilChanged() = runTest {
        val flow = flowOf(1, 1, 2, 2, 1).distinctUntilChanged()
        assertEquals(4, flow.sum())
    }

    @Test
    fun testDistinctUntilChangedKeySelector() = runTest {
        val flow = flow {
            emit(Box(1))
            emit(Box(1))
            emit(Box(2))
            emit(Box(1))
        }

        val sum1 = flow.distinctUntilChanged().map { it.i }.sum()
        val sum2 = flow.distinctUntilChanged(Box::i).map { it.i }.sum()
        assertEquals(5, sum1)
        assertEquals(4, sum2)
    }

    @Test
    fun testThrowingKeySelector() = runTest {
        var cancelled = false
        val flow = flow {
            coroutineScope {
                launch {
                    hang {cancelled = true}
                }
                yield()
                emit(1)
            }
        }.distinctUntilChanged { throw TestException() }

        assertFailsWith<TestException>(flow)
        assertTrue(cancelled)
    }
}