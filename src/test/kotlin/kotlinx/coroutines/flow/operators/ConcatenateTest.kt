package kotlinx.coroutines.flow.operators

import examples.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class ConcatenateTest : TestBase() {
    @Test
    fun testConcatenate() = runTest {
        val n = 100
        val sum = flow {
            repeat(n) {
                emit(it + 1) // 1..100
            }
        }.map { value ->
            flow {
                repeat(value) {
                    emit(it + 1)
                }
            }
        }.concatenate().sum()

        assertEquals(n * (n + 1) * (n + 2) / 6, sum)
    }

    @Test
    fun testSingle() = runTest {
        val flows = flow {
            repeat(100) {
                if (it == 99) emit(flowOf(42))
                else emit(flowOf())
            }
        }

        val value = flows.concatenate().single()
        assertEquals(42, value)
    }


    @Test
    fun testContext() = runTest {
        val captured = ArrayList<String>()
        val flow = flow {
            emit(flow {
                captured += captureName()
                emit(1)
            }.flowOn(named("first")))

            emit(flow {
                captured += captureName()
                emit(1)
            }.flowOn(named("second")))
        }.concatenate().flowOn(named("irrelevant"))

        assertEquals(2, flow.sum())
        assertEquals(listOf("first", "second"), captured)
    }
}