package examples

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.terminal.*
import kotlin.concurrent.*

/*
 * This example demonstrates integration with callback-based API.
 * It is similar to any hot-based stream that is usually adapted with FluxSink or its analogues
 */
interface CallbackBasedApi {
    fun register(callback: Callback)
    fun unregister(callback: Callback)
}

interface Callback {
    fun onNextEventFromExternalApi(event: Int)
    fun onExceptionFromExternalApi(throwable: Throwable)
}

// This is the main entry point to callback based API
fun CallbackBasedApi.flow(): Flow<Int> = flowViaChannel { channel ->
    val adapter = FlowSinkAdapter(channel)
    register(adapter)
    channel.invokeOnClose {
        unregister(adapter)
    }
}

private class FlowSinkAdapter(private val sink: SendChannel<Int>) : Callback {

    override fun onNextEventFromExternalApi(event: Int) {
        /*
         * Channel can be closed at the moment of offer, in that case caller
         * should decide what to do in an application-specific manner.
         * See issues #974 and #1042
         */
        runCatching { sink.offer(event) }
    }

    override fun onExceptionFromExternalApi(throwable: Throwable) {
        sink.close(throwable)
    }
}

object InfiniteApiInstance : CallbackBasedApi {

    @Volatile
    private var unregistered = false

    override fun register(callback: Callback) {
        println("Callback ${callback.javaClass.name} is successfully registered")
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
        println("Callback ${callback.javaClass.name} is successfully unregistered")
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
        .limit(3)

    println("Flow prepared")

    var elements = 0
    withContext(consumptionContext) {
        flow.collect { value ->
            println("Received $value on ${Thread.currentThread()}")
            if (++elements > 4) throw RuntimeException() // Never happens
        }
    }
}
