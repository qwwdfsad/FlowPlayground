package examples

import flow.*
import flow.operators.*
import flow.sink.*
import flow.terminal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.debug.*
import kotlin.concurrent.*


fun CallbackBasedApi.sinkFlow(): Flow<Int> = FlowSink.create { sink ->
    val adapter = FlowSinkAdapter(sink)
    register(adapter)
    sink.join()
    unregister(adapter)
}

private class FlowSinkAdapter(private val sink: FlowSink<Int>) : Callback {

    override fun onNextEventFromExternalApi(event: Int) {
        sink.onNext(event)
    }

    override fun onExceptionFromExternalApi(throwable: Throwable) {
        sink.onException(throwable)
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
        println("Callback ${callback.javaClass.name} successfully unregistered")
    }
}

private val consumptionContext = newSingleThreadContext("Consumer")
fun main() {
    DebugProbes.install()
    val flow = InfiniteApiInstance.sinkFlow()
        .map { it }
        .withDownstreamContext(consumptionContext)

    println("Flow prepared")
    var elements = 0
    flow.limit(10)
        .consumeOn(consumptionContext) {
            println("Received $it on thread ${Thread.currentThread()}")
            if (++elements > 5) throw CancellationException()
        }
}