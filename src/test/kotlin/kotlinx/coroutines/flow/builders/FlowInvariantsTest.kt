package kotlinx.coroutines.flow.builders

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FlowInvariantsTest : TestBase() {

    @Test
    fun testWithContextDoesNotChangeExecution() = runTest {
        val flow = flow {
            emit(captureName())
        }.flowOn(named("original"))

        var result = "unknown"
        withContext(named("misc")) {
            flow
                .flowOn(named("upstream"))
                .launchIn(this + named("consumer")) {
                    onEach {
                        result = it
                    }
                }.join()
        }

        assertEquals("original", result)
    }
}