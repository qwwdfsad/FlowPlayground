package kotlinx.coroutines.flow.reactivestreams


import kotlinx.coroutines.*
import kotlinx.coroutines.flow.terminal.*
import reactor.core.publisher.*
import kotlin.test.*

class PublisherAsFlowTest {

    @Test
    fun testCancellation() = runBlocking<Unit> {
        var onNext = 0
        var onCancelled = 0
        var onError = 0

        Flux.fromArray(Array(100) { it }).doOnCancel {
            ++onCancelled
        }.asFlow().consumeOn(Dispatchers.Unconfined, onError = { ++onError; println(it) }) {
            ++onNext
            throw RuntimeException()
        }.join()

        assertEquals(1, onNext)
        assertEquals(1, onError)
        assertEquals(1, onCancelled)
    }
}