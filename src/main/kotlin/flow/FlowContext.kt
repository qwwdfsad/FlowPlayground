package flow

import flow.operators.*
import io.reactivex.*
import io.reactivex.schedulers.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*


val t get() = Thread.currentThread()!!

suspend fun main2() {
    val f = { println("Doing IO operation in $t"); 42}.flow()

    f.map {
        println("Mapping I/O result in $t")
    }.flowBridge {
        println("Consuming I/O result in $t")
    }
}

fun <T> Flow<T>.withUpstreamContext(coroutineContext: CoroutineContext): Flow<T> = TODO()

fun <T> Flow<T>.withDownstreamContext(coroutineContext: CoroutineContext, bufferSize: Int = 16): Flow<T> = flow {
    val channel = Channel<T>(bufferSize)

    val job = GlobalScope.launch(coroutineContext) {
        for (element in channel) {
            // TODO exception
            push(element)
        }
    }

    flowBridge {
        channel.send(it)
    }
}

suspend fun main() {
    val computation = { println("Computing in $t"); 42 }

    computation.flow()
        .withDownstreamContext(newSingleThreadContext("Test Context"))
        .map {
            println("Mapping in $t")
            it
        }
        .withDownstreamContext(Dispatchers.Default)
        .flowBridge {
            println("Consuming in $t")
        }

    delay(1000)
    println("It's over")
    System.exit(-1)
}

private fun rxExample() {
    val obs = Flowable.fromCallable {
        println("Doing IO operation in $t")
        42
    }

    obs.observeOn(Schedulers.newThread(), false, 32) // <- uhm, changes downstream ctx
        .subscribeOn(Schedulers.io()) // <- uhm x2, changes upstream ctx
        .map { println("Mapping I/O result in $t") }
        .subscribe {
            println("Consuming I/O result in $t")
        }

    Thread.sleep(10000)
}
