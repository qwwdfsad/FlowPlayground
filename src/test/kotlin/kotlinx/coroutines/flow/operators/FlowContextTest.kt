package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import kotlinx.coroutines.sync.*
import org.junit.Test
import kotlin.test.*

class FlowContextTest : TestBase() {

    @Test
    fun testMixedContext() = runTest {
        val captured = ArrayList<String>()
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
            .withDownstreamContext(named("ignored"))
            .awaitSingle()

        assertEquals(314, value)
        assertEquals(listOf("upstream", "upstream", "upstream", "downstream", "downstream"), captured)
    }

    @Test
    fun testMixedContextAndException() = runTest {
        val captured = ArrayList<String>()

        val mutex = Mutex()
        val flow = flow {
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

        repeat(5) {
            state = 0
            needle = it + 1

            assertFailsWith<TestException> {
                flow
                    .map(mapper) // 1
                    .withUpstreamContext(named("upstream"))
                    .map(mapper) // 2
                    .withDownstreamContext(named("downstream"))
                    .map(mapper) // 3
                    .withUpstreamContext(named("upstream2"))
                    .map(mapper) // 4
                    .withDownstreamContext(named("downstream2"))
                    .map(mapper) // 5
                    .awaitSingle()
            }
        }
    }
}
