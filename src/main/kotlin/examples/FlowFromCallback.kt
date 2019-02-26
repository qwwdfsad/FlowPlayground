package examples

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.sink.*
import kotlinx.coroutines.flow.terminal.*
import kotlin.concurrent.*

interface CallbackBasedApi {
    fun register(callback: Callback)
    fun unregister(callback: Callback)
}

interface Callback {
    fun onNextEventFromExternalApi(event: Int)
    fun onExceptionFromExternalApi(throwable: Throwable)
}

// This is the main entry point
fun CallbackBasedApi.flow(): Flow<Int> = flowViaSink { sink ->
    val adapter = FlowSinkAdapter(sink)
    register(adapter)
    sink.join() // Join never throws, just waits until either producer or consumer finishes
    unregister(adapter)
}

private class FlowSinkAdapter(private val sink: FlowSink<Int>) : Callback {

    override fun onNextEventFromExternalApi(event: Int) {
        sink.offer(event)
    }

    override fun onExceptionFromExternalApi(throwable: Throwable) {
        sink.completeExceptionally(throwable)
    }
}

object InfiniteApiInstance : CallbackBasedApi {

    @Volatile
    private var unregistered = false

    override fun register(callback: Callback) {
        println("Callback ${callback.javaClass.name} successfully registered")
        thread {
            var i = 0
            while (!unregistered) {
                Thread.sleep(100)
                callback.onNextEventFromExternalApi(++i)
            }
        }
    }

    override fun unregister(callback: Callback) {
        unregistered = true
        println("Callback ${callback.javaClass.name} successfully unregistered from thread")
    }
}

suspend fun main() {
    val mapperContext = newSingleThreadContext("Mapper")
    val consumptionContext = newSingleThreadContext("Consumer")

    val flow = InfiniteApiInstance.flow()
        .map {
            println("Mapping $it on ${Thread.currentThread()}")
            it
        }
        .flowOn(mapperContext)

    println("Flow prepared")
    var elements = 0
    flow.limit(3)
        .consumeOn(consumptionContext, onError = { t -> println("Handling $t") }) {
            println("Received $it on ${Thread.currentThread()}")
            if (++elements > 5) throw RuntimeException()
        }.join()
}
