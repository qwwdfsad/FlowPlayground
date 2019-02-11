package flow.channels

import flow.operators.*
import flow.source.*
import flow.terminal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun <T : Any> BroadcastChannel<T>.flow() = flow<T> {
    openSubscription().consumeEach {
        push(it)
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

    val flow = bc.flow().limit(10).filter { it % 2 == 0 }
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
