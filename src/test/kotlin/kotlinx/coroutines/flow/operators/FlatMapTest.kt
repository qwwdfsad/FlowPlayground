package kotlinx.coroutines.flow.operators

import examples.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FlatMapTest : TestBase() {

    @Test
    fun testFlatMap() = runTest {
        val n = 100
        val sum = flow {
            repeat(n) {
                emit(it + 1) // 1..100
            }
        }.flatMap { value ->
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
        }.flatMap { value ->
            if (value == 99) flowOf(42)
            else flowOf()
        }

        val value = flow.single()
        assertEquals(42, value)
    }

    @Test
    fun testFailureCancellation() = runTest {
        var finally = false
        val flow = flow {
            emit(1)
            emit(2)
        }.flatMap {
            if (it == 1) flow {
                hang {finally = true}
            } else flow<Int> {
                throw TestException()
            }
        }

        assertFailsWith<TestException>(flow.count())
        assertTrue(finally)
    }

    @Test
    fun testFailureInMapOperationCancellation() = runTest {
        var finally = false
        val latch = Channel<Unit>()
        val flow = flow {
            emit(1)
            emit(2)
        }.flatMap {
            if (it == 1) flow<Int> {
                latch.send(Unit)
                hang { finally = true }
            } else {
                latch.receive()
                throw TestException()
            }
        }

        assertFailsWith<TestException>(flow.count())
        assertTrue(finally)
    }

    @Test
    fun testConcurrentFailure() = runTest {
        var finally = false
        val latch = Channel<Unit>()

        val flow = flow {
            emit(1)
            emit(2)
        }.flatMap {
            if (it == 1) flow<Int> {
                latch.send(Unit)
                hang {
                    finally = true
                    throw RuntimeException() // TODO TE2

                }
            } else {
                latch.receive()
                throw TestException()
            }
        }

        assertFailsWith<TestException>(flow) // TODO stacktrace recovery in coroutines
        assertTrue(finally)
    }

    @Test
    fun testContext() = runTest {
        val captured = ArrayList<String>()
        val flow = flowOf(1)
            .flowOn(named("irrelevant"))
            .flatMap {
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
                flatMap {
                    flow {
                        captured += captureName()
                        emit(it)
                    }
                }
            }.flowOn(named("irrelevant"))
            .flatMap {
                flow {
                    captured += captureName()
                    emit(it)
                }
            }.flowOn(named("outer"))

        flow.single()
        assertEquals(listOf("inner", "outer"), captured)
    }
}
