package kotlinx.coroutines.flow.operators

import examples.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class ConcatenateMapTest : TestBase() {
    @Test
    fun testConcatenate() = runTest {
        val n = 100
        val sum = flow {
            repeat(n) {
                emit(it + 1) // 1..100
            }
        }.concatenate { value ->
            // 1 + (1 + 2) + (1 + 2 + 3) + ... (1 + .. + n)
            flow {
                repeat(value) {
                    emit(it + 1)
                }
            }
        }.sum()

        assertEquals(n * (n + 1) * (n + 2) / 6, sum)
    }

    @Test
    fun testSingle() = runTest {
        val flow = flow {
            repeat(100) {
                emit(it)
            }
        }.concatenate { value ->
            if (value == 99) flowOf(42)
            else flowOf()
        }

        val value = flow.single()
        assertEquals(42, value)
    }

    @Test
    fun testFailure() = runTest {
        var finally = false
        val latch = Channel<Unit>()
        val flow = flow {
            coroutineScope {
                launch {
                    latch.send(Unit)
                    hang { finally = true }
                }

                emit(1)
            }
        }.concatenate {
            flow<Int> {
                latch.receive()
                throw TestException()
            }
        }

        assertFailsWith<TestException>(flow.count())
        assertTrue(finally)
    }

    @Test
    fun testFailureInMapOperation() = runTest {
        var finally = false
        val latch = Channel<Unit>()
        val flow = flow {
            coroutineScope {
                launch {
                    latch.send(Unit)
                    hang { finally = true }
                }

                emit(1)
            }
        }.concatenate<Int, Int> {
            latch.receive()
            throw TestException()
        }

        assertFailsWith<TestException>(flow.count())
        assertTrue(finally)
    }

    @Test
    fun testContext() = runTest {
        val captured = ArrayList<String>()
        val flow = flowOf(1)
            .flowOn(named("irrelevant"))
            .concatenate {
                flow {
                    captured += captureName()
                    emit(it)
                }
            }

        flow.flowOn(named("1")).sum()
        flow.flowOn(named("2")).sum()
        assertEquals(listOf("1", "2"), captured)
    }


    @Test
    fun testIsolatedContext() = runTest {
        val captured = ArrayList<String>()
        val flow = flowOf(1)
            .flowOn(named("irrelevant"))
            .flowWith(named("inner")) {
                concatenate {
                    flow {
                        captured += captureName()
                        emit(it)
                    }
                }
            }.flowOn(named("irrelevant"))
            .concatenate {
                flow {
                    captured += captureName()
                    emit(it)
                }
            }.flowOn(named("outer"))

        flow.single()
        assertEquals(listOf("inner", "outer"), captured)
    }
}