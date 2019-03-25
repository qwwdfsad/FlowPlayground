package kotlinx.coroutines.flow.operators

import examples.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class DropTest : TestBase() {
    @Test
    fun testDrop() = runTest {
        val flow = flow {
            emit(1)
            emit(2)
            emit(3)
        }

        assertEquals(5, flow.drop(1).sum())
        assertEquals(0, flow.drop(Int.MAX_VALUE).sum())
        assertNull(flow.drop(Int.MAX_VALUE).singleOrNull())
        assertEquals(3, flow.drop(1).take(2).drop(1).single())
    }

    @Test
    fun testEmptyFlow() = runTest {
        val sum = flowOf<Int>().drop(1).sum()
        assertEquals(0, sum)
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
                emit(2)
            }
        }.drop(1)
            .map {
                throw TestException()
                42
            }.onErrorReturn(42)

        assertEquals(42, flow.single())
        assertTrue(cancelled)
    }
}
