package examples

import flow.*
import flow.operators.*
import flow.source.*
import flow.terminal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.concurrent.*

/**
 *
 */
interface CallbackBasedApi {
    fun register(callback: Callback)
    fun unregister(callback: Callback)
}

interface Callback {
    fun onNextEventFromExternalApi(event: Int)
    fun onExceptionFromExternalApi(throwable: Throwable)
}

fun CallbackBasedApi.flow(): Flow<Int> {
    return flow {
        val channel = Channel<Int>()
        val callback = FlowAdapterCallback(channel)
        register(FlowAdapterCallback(channel))

        try {
            for (i in channel) {
                push(i)
            }
        } finally {
            unregister(callback)
        }
    }
}

private class FlowAdapterCallback(private val channel: Channel<Int>) : Callback {

    override fun onNextEventFromExternalApi(event: Int) {
        channel.sendBlocking(event)
    }

    override fun onExceptionFromExternalApi(throwable: Throwable) {
        channel.close(throwable)
    }
}

object ApiInstance : CallbackBasedApi {
    override fun register(callback: Callback) {
        println("Callback ${callback.javaClass.name} successfully registered")
        thread {
            Thread.sleep(100)
            callback.onNextEventFromExternalApi(1)
            Thread.sleep(100)
            callback.onNextEventFromExternalApi(2)
            callback.onExceptionFromExternalApi(Throwable("External API failed"))
        }
    }

    override fun unregister(callback: Callback) {
        println("Callback ${callback.javaClass.name} successfully unregistered")
    }
}

private val consumptionContext = newSingleThreadContext("Consumer")
fun main() {
    val flow = ApiInstance.flow()
        .map { it + 1 }
        .withDownstreamContext(consumptionContext)

    println("Flow prepared")
    flow.consumeOn(consumptionContext) {
        println("Received $it on thread ${Thread.currentThread()}")
    }
}
