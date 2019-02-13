package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.operators.*
import org.junit.Test
import kotlin.test.*

class ToCollectionTest : TestBase() {

    private val flow = flow {
        repeat(10) {
            push(42)
        }
    }

    private val emptyFlow = flow<Int>()

    @Test
    fun testToList() = runTest {
        assertEquals(List(10) { 42 }, flow.toList())
        assertEquals(emptyList(), emptyFlow.toList())
    }

    @Test
    fun testToSet() = runTest {
        assertEquals(setOf(42), flow.toSet())
        assertEquals(emptySet(), emptyFlow.toSet())
    }
}