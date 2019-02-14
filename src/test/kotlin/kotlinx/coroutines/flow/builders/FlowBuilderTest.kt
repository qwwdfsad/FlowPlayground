package kotlinx.coroutines.flow.builders

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FlowBuilderTest : TestBase() {


    @Test
    fun testWithContextDoesNotChangeExecution() = runTest {
        val flow = flow(named("original")) {
            emit(captureName())
        }

        var result: String = "unknown"
        withContext(named("misc")) {
            flow
                .withUpstreamContext(named("upstream"))
                .consumeOn(named("consumer")) {
                    result = it
                }.join()
        }

        assertEquals("original", result)
    }
}