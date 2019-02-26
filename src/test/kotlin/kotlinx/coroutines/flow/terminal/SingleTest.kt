package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import org.junit.Test
import kotlin.test.*

class SingleTest : TestBase() {

    @Test
    fun testSingle() = runTest {
        val result = flow {
            emit(239L)
        }.single()

        assertEquals(239L, result)
    }

    @Test
    fun testMultipleValues() = runTest {
        assertFailsWith<RuntimeException> {
            flow {
                emit(239L)
                emit(240L)
            }.single()
        }
    }

    @Test
    fun testNoValues() = runTest {
        assertFailsWith<NoSuchElementException> {
            flow<Int> {}.single()
        }
    }

    @Test
    fun testException() = runTest {
        assertFailsWith<TestException> {
            flow<Int> {
                throw TestException()
            }.single()
        }

        assertFailsWith<TestException> {
            flow {
                emit(1)
                throw TestException()
            }.single()
        }
    }
}
