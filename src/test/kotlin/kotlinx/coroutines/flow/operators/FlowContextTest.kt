package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import org.junit.Test
import kotlin.test.*

class FlowContextTest : TestBase() {

    private val captured = ArrayList<String>()

    @Test
    fun testMixedContext() = runTest("main") {
        val flow = flow {
            captured += captureName()
            emit(314)
        }

        val mapper: suspend (Int) -> Int = {
            captured += captureName()
            it
        }

        val value = flow // upstream
            .map(mapper) // upstream
            .flowOn(named("upstream"))
            .map(mapper) // upstream 2
            .flowWith(named("downstream")) {
                map(mapper) // downstream
            }
            .flowOn(named("upstream 2"))
            .map(mapper) // main
            .single()

        assertEquals(314, value)
        assertEquals(listOf("upstream", "upstream", "upstream 2", "downstream", "main"), captured)
    }

    @Test
    fun testExceptionReporting() = runTest("main") {
        val flow = flow {
            captured += captureName()
            emit(314)
            delay(Long.MAX_VALUE)
        }.flowOn(named("upstream"))
            .map {
                throw TestException()
            }

        assertFailsWith<TestException> { flow.single() }
        assertFailsWith<TestException>(flow)
        ensureActive()
    }

    @Test
    fun testMixedContextsAndException() = runTest("main") {
        val baseFlow = flow {
            captured += captureName()
            emit(314)
            delay(Long.MAX_VALUE)
        }

        var state = 0
        var needle = 1
        val mapper: suspend (Int) -> Int = {
            if (++state == needle) throw TestException()
            it
        }

        val flow = baseFlow.map(mapper) // 1
            .flowOn(named("ctx 1"))
            .map(mapper) // 2
            .flowWith(named("ctx 2")) {
                map(mapper) // 3
            }
            .map(mapper) // 4
            .flowOn(named("ctx 3"))
            .map(mapper) // 5

        repeat(5) {  // Will hang for 6
            state = 0
            needle = it + 1
            assertFailsWith<TestException> { flow.single() }

            state = 0
            assertFailsWith<TestException>(flow)
        }

        ensureActive()
    }

    @Test
    fun testNestedContexts() = runTest("main") {
        val mapper: suspend (Int) -> Int = { captured += captureName(); it }
        val value = flow {
            captured += captureName()
            emit(1)
        }.flowWith(named("outer")) {
            map(mapper)
                .flowOn(named("nested first"))
                .flowWith(named("nested second")) {
                    map(mapper)
                        .flowOn(named("inner first"))
                        .map(mapper)
                }
                .map(mapper)
        }.map(mapper)
            .single()
        val expected = listOf("main", "nested first", "inner first", "nested second", "outer", "main")
        assertEquals(expected, captured)
        assertEquals(1, value)
    }


    @Test
    fun testFlowContextCancellation() = runTest("main") {
        val latch = Channel<Unit>()
        var invoked = false
        val flow = flow {
            try {
                emit(1)
            } finally {
                invoked = true
            }
        }.flowWith(named("outer")) {
            map { it }
                .flowOn(named("inner"))

        }.map {
            latch.send(Unit)
            delay(Long.MAX_VALUE)
        }.flowOn(named("delayed"))

        val job = launch {
            flow.single()
        }

        latch.receive()
        job.cancelAndJoin()
        assertTrue(invoked)
        ensureActive()
    }
}
