package kotlinx.coroutines.flow.builders

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FlowInvariantsTest : TestBase() {

    @Test
    fun testWithContextContract() = runTest {
        flow {
            kotlinx.coroutines.withContext(NonCancellable) { // This one cannot be prevented :(
                emit(1)
            }
        }.collect {
            assertEquals(1, it)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testWithContextContractViolated() = runTest {
        flow {
            kotlinx.coroutines. withContext(named("foo")) {
                emit(1)
            }
        }.collect {
            fail()
        }
    }

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