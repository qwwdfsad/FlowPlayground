package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FlowContextDownstreamTest : TestBase() {

    @Test
    fun testWithDownstreamContext() = runTest {
        val captured = ArrayList<String>()
        val flow = flow {
            captured += captureName()
            push(314)
        }

        val mapper: suspend (Int) -> Int = {
            captured += captureName()
            it
        }

        val result = flow.withDownstreamContext(named("ctx1"))
            .map(mapper)
            .withDownstreamContext(named("ctx2"))
            .map(mapper)
            .awaitSingle()

        assertEquals(314, result)
        assertEquals(listOf("main", "ctx1", "ctx2"), captured)
    }

    @Test
    public fun testWithDownstreamThrowingSource() = runTest {
        val flow = flow {
            push(captureName())
            throw TestException()
        }

        assertFailsWith<TestException> { flow.map { it }.withDownstreamContext(named("throwing")).awaitSingle() }
    }

    @Test
    public fun testWithDownstreamThrowingOperator() = runTest {
        val flow = flow {
            push(captureName())
            delay(Long.MAX_VALUE)
        }

        assertFailsWith<TestException> {
            flow
                .map { throw TestException(); it }
                .withDownstreamContext(named("throwing"))
                .awaitSingle()
        }
    }

    @Test
    public fun testWithDownstreamThrowingDownstreamOperator() = runTest {
        val flow = flow {
            push(captureName())
            delay(Long.MAX_VALUE)
        }

        assertFailsWith<TestException> {
            flow
                .map { it }
                .withDownstreamContext(named("throwing"))
                .map { throw TestException() }
                .awaitSingle()
        }
    }
}