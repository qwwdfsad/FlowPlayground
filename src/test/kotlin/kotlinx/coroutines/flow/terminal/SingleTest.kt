package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import org.junit.Test
import kotlin.test.*

class SingleTest : TestBase() {

    @Test
    fun testSingle() = runTest {
        val result = flow {
            push(239L)
        }.awaitSingle()

        assertEquals(239L, result)
    }

    @Test
    fun testMultipleValues() = runTest {
        assertFailsWith<RuntimeException> {
            flow {
                push(239L)
                push(240L)
            }.awaitSingle()
        }
    }

    @Test
    fun testNoValues() = runTest {
        assertFailsWith<NoSuchElementException> {
            flow<Int> {}.awaitSingle()
        }
    }

    @Test
    fun testException() = runTest {
        assertFailsWith<TestException> {
            flow<Int> {
                throw TestException()
            }.awaitSingle()
        }

        assertFailsWith<TestException> {
            flow {
                push(1)
                throw TestException()
            }.awaitSingle()
        }
    }
}
