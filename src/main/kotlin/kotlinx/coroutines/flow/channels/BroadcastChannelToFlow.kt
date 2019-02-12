package kotlinx.coroutines.flow.channels

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

fun <T : Any> BroadcastChannel<T>.asFlow() = flow<T> {
    val subscription = openSubscription()
    try {
        for (element in subscription) {
            push(element)
        }
    } catch (e: Throwable) {
        if (e is CancellationException) {
            subscription.cancel()
        } else {
            subscription.cancel(e)
            throw e
        }
    }
}

fun <T : Any> Flow<T>.asChannel(): BroadcastChannel<T> = GlobalScope.broadcast(Dispatchers.Unconfined) {
    flowBridge { value ->
        send(value)
    }
}

suspend fun main() {
    val bc = GlobalScope.broadcast {
        var i = 0
        while (true) {
            send(++i)
            delay(20)
        }
    }

    val flow = bc.asFlow().limit(10).filter { it % 2 == 0 }
    println("Flow prepared, sleeping for 100 ms")
    delay(100)

    flow.consumeOn(Dispatchers.Unconfined) {
        println(it)
    }.join()

    println("Flow consumed, sleeping 100 ms more")
    delay(100)
    println("Starting to consume flow again")
    flow.consumeOn(Dispatchers.Unconfined) {
        println(it)
    }.join()
}
