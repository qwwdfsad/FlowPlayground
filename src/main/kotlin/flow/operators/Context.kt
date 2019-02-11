package flow.operators

import flow.*
import flow.source.*
import flow.terminal.*
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

fun <T : Any> Flow<T>.withUpstreamContext(coroutineContext: CoroutineContext): Flow<T> = flow {
    withContext(coroutineContext) {
        flowBridge {
            push(it)
        }
    }
}

public fun <T : Any> Flow<T>.withDownstreamContext(downstreamContext: CoroutineContext, bufferSize: Int = 16): Flow<T> =
    flow {
        val channel = Channel<T>(bufferSize)

        coroutineScope {
            launch(downstreamContext) {
                for (element in channel) {
                    try {
                        push(element)
                    } catch (e: Throwable) {
                        channel.close(e)
                    }
                }
            }

            try {
                flowBridge {
                    channel.send(it)
                }
            } finally {
                channel.close()
            }
        }
    }

suspend fun main() {
    val computation = { println("Computing in $t"); 42 }
    val upstreamContext = newSingleThreadContext("Upstream context")
    val intermediateContext = newSingleThreadContext("Intermediate context")
    val downstreamContext = newSingleThreadContext("Downstream context")

    val j = computation.flow()
        .filter {
            println("Filtering in $t")
            true
        }
        .withUpstreamContext(upstreamContext)
        .withDownstreamContext(intermediateContext)
        .map {
            println("Mapping in $t")
            it
        }.consumeOn(downstreamContext) {
            println("Consuming in $t")
            error("f")
        }

    j.join()
}

private fun rxExample() {
    val upstreamContext = newSingleThreadContext("Upstream context")
    val intermediateContext = newSingleThreadContext("Intermediate context")
    val downstreamContext = newSingleThreadContext("Downstream context")

    val flowable = Flowable.fromCallable {
        println("Doing IO operation in $t")
        42
    }

    flowable
        .filter {
            println("Filtering in $t")
            true
        }
        .subscribeOn(upstreamContext.scheduler) // changes upstream ctx
        .observeOn(intermediateContext.scheduler, false, 32) // <- changes downstream ctx
        .map { println("Mapping in $t") }
        .observeOn(downstreamContext.scheduler, false, 32) // <- changes downstream ctx
        .subscribe {
            println("Consuming in $t")
        }

    Thread.sleep(10000)
    System.exit(-1)
}

val ExecutorCoroutineDispatcher.scheduler get () = Schedulers.from(executor)