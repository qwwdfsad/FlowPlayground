package flow

import flow.operators.*
import io.reactivex.*
import io.reactivex.schedulers.*

inline fun <T> flow(crossinline block: suspend FlowSubscriber<T>.() -> Unit) = object : Flow<T> {
    override suspend fun subscribe(consumer: FlowSubscriber<T>) = consumer.block()
}

inline fun <T> (() -> T).flow(): Flow<T> = object : Flow<T> {
    override suspend fun subscribe(consumer: FlowSubscriber<T>) {
        val result = this@flow()
        consumer.push(result)
    }
}

fun <T> Iterable<T>.asFlow(): Flow<T> = flow {
    forEach { value ->
        push(value)
    }
}

fun <T> flow(vararg elements: T) = elements.asIterable().asFlow()

val t get() = Thread.currentThread()!!

suspend fun main() {
    val f = { println("Doing IO operation in $t"); 42}.flow()

    f.map {
        println("Mapping I/O result in $t")
    }.consumeEach {
        println("Consuming I/O result in $t")
    }
}



fun main2() {
    val obs = Flowable.defer {
        Flowable.fromCallable {
            println("I am in thread: $t")
            42
        }
    }

    obs.subscribeOn(Schedulers.io()) // <- uhm x2
        .observeOn(Schedulers.newThread()) // <- uhm
        .subscribe {
            println("Yay: $it on $t")
        }

    Thread.sleep(10000)
}