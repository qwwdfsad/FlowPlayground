package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FlowContextTest : TestBase() {

    private val captured = ArrayList<String>()

    @Test
    fun testMixedContext() = runTest {
        val flow = flow {
            captured += captureName()
            push(314)
        }

        val mapper: suspend (Int) -> Int = {
            captured += captureName()
            it
        }

        val value = flow
            .map(mapper)
            .withUpstreamContext(named("upstream"))
            .map(mapper)
            .withDownstreamContext(named("downstream"))
            .map(mapper)
            .withUpstreamContext(named("ignored"))
            .map(mapper)
            .withDownstreamContext(named("downstream 2"))
            .map(mapper)
            .awaitSingle()

        assertEquals(314, value)
        assertEquals(listOf("upstream", "upstream", "upstream", "downstream", "downstream", "downstream 2"), captured)
    }

    @Test
    fun testExceptionReporting() = runTest {
        val flow = flow(named("upstream")) {
            captured += captureName()
            push(314)
            delay(Long.MAX_VALUE)
        }.map {
            throw TestException()
        }.withDownstreamContext(named("downstream"))

        assertFailsWith<TestException> { flow.awaitSingle() }
        assertFailsWith<TestException>(flow)
    }

    @Test
    fun testMixedContextAndException() = runTest {
        val baseFlow = flow {
            captured += captureName()
            push(314)
            delay(Long.MAX_VALUE)
        }

        var state = 0
        var needle = 1
        val mapper: suspend (Int) -> Int = {
            if (++state == needle) throw TestException()
            it
        }

        val flow = baseFlow.map(mapper) // 1
            .withUpstreamContext(named("upstream"))
            .map(mapper) // 2
            .withDownstreamContext(named("downstream"))
            .map(mapper) // 3
            .withUpstreamContext(named("upstream2"))
            .map(mapper) // 4
            .withDownstreamContext(named("downstream2"))
            .map(mapper) // 5

        repeat(5) {  // Will hang for 6
            state = 0
            needle = it + 1
            assertFailsWith<TestException> { flow.awaitSingle() }

            state = 0
            assertFailsWith<TestException>(flow)
        }
    }
}
