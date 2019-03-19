package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.flow.*
import org.junit.*

class LaunchFlowDslTest : TestBase() {

    @Test
    fun testValidBuilder() = testDsl {
        onEach {}
        catch<TestException> {}
        catch<Error> {}
        finally {}
    }

    @Test
    fun testNoCatch() = testDsl {
        onEach {}
        finally {}
    }

    @Test
    fun testNoFinally() = testDsl {
        onEach {}
        finally {}
    }

    @Test(expected = IllegalStateException::class)
    fun testNoOnEach() = testDsl {
        catch<TestException> {}
        finally {}
    }

    @Test(expected = IllegalStateException::class)
    fun testInvalidCatchOrder() = testDsl {
        onEach {}
        catch<Throwable> {}
        catch<TestException> {}
        finally {}
    }

    @Test(expected = IllegalStateException::class)
    fun testOnEachLast() = testDsl {
        catch<TestException> {}
        finally {}
        onEach {}
    }

    @Test(expected = IllegalStateException::class)
    fun testFinallyBeforeCatch() = testDsl {
        onEach {}
        finally {}
        catch<TestException> {}
    }

    @Test(expected = IllegalStateException::class)
    fun testOnEachInTheMiddle() = testDsl {
        catch<TestException> {}
        onEach {}
        finally {}
    }

    @Test(expected = IllegalStateException::class)
    fun testEmpty() = testDsl {
    }

    private fun testDsl(block: LaunchFlowBuilder<Unit>.() -> Unit) {
        val builder = LaunchFlowBuilder<Unit>()
        builder.block()
        builder.build()
    }
}