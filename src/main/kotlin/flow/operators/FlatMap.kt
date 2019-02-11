package flow.operators

import flow.*
import flow.source.*
import io.reactivex.*
import io.reactivex.functions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

fun <T : Any, R : Any> Flow<T>.flatMap(mapper: (T) -> Flow<R>): Flow<R> = flow {
    // Let's try to leverage the fact that flatMap is never contended
    val flatMap = FlatMapFlow(this)
    val root = CompletableDeferred<Unit>()
    flowBridge {
        val inner = mapper(it)
        GlobalScope.launch(root + Dispatchers.Unconfined) {
            inner.flowBridge { value ->
                flatMap.push(value)
            }
        }
    }

    root.complete(Unit)
    root.await()
}

private class FlatMapFlow<T>(private val downstream: FlowSubscriber<T>) {
    private val channel: Channel<T> by lazy { Channel<T>(16) }
    private val inProgress = AtomicBoolean(false)

    suspend fun push(value: T) {
        if (!inProgress.compareAndSet(false, true)) {
            channel.send(value)
            // TODO after a while it does not look that reliable
            if (inProgress.compareAndSet(false, true)) {
                helpPush()
            }
            return
        }

        downstream.push(value)
        helpPush()
    }

    private suspend fun helpPush() {
        var element = channel.poll()
        while (element != null) {
            downstream.push(element)
            element = channel.poll()
        }

        inProgress.set(false)
    }
}

suspend fun flowFlatMap() {
    val base = System.currentTimeMillis()
    flow(3, 4, 5).flatMap {
        val index = it
        val inner = Array(it) { "$index flowable, $it's element" }.asIterable().asFlow()
        inner.delayEach(if (it == 5) it * 500L else it * 1000L)
    }.flowBridge {
        val diff = (System.currentTimeMillis() - base) / 1000 * 1000
        println("$it, ($diff)")
    }
}

fun <T> Flowable<T>.delayEach(interval: Long, timeUnit: TimeUnit): Flowable<T> =
    Flowable.zip(this, Flowable.interval(interval, timeUnit), BiFunction { item, _ -> item })

suspend fun main() {
    println("Flow flat map:")
    flowFlatMap()
    println("\n\nFlowable flat map:")
    reactiveFlatMap()
}

private fun reactiveFlatMap() {
    val latch = CountDownLatch(1)
    val base = System.currentTimeMillis()
    val flowable = Flowable.just(3, 4, 5).flatMap {
        val index = it
        val f = Flowable.fromArray(*Array(it) { "$index flowable, $it's element" })
            .delayEach(if (it == 5) it * 500L else it * 1000L, TimeUnit.MILLISECONDS)
        f
    }

    flowable.subscribe({
        val diff = (System.currentTimeMillis() - base) / 1000 * 1000
        println("$it, ($diff)")
    }, { println(it) }, { latch.countDown() })
    latch.await()
}
