package kotlinx.coroutines.flow.operators

import io.reactivex.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*
import kotlinx.coroutines.selects.*
import java.util.concurrent.*
import kotlin.coroutines.*

// TODO this one should be rewritten?
public fun <T : Any> Flow<T>.debounce(timeoutMillis: Long): Flow<T> =
    flow {
        val conflated = Channel<T>(Channel.CONFLATED)
        val job = GlobalScope.async(coroutineContext) {
            var lastReceived: T? = null
            whileSelect {
                conflated.onReceiveOrNull {
                    if (it !== null) {
                        lastReceived = it
                        true
                    } else {
                        false
                    }
                }

                onTimeout(timeoutMillis) {
                    if (lastReceived != null) {
                        emit(lastReceived!!)
                    }
                    true
                }
            }

            if (lastReceived != null) {
                emit(lastReceived!!)
            }

            lastReceived
        }

        collect {
            conflated.send(it)
        }

        // "close with value" idiom
        val lastValue = conflated.poll()
        conflated.close()
        val lastPushedValue = job.await()
        if (lastPushedValue != lastValue && lastValue != null) {
            emit(lastValue)
        }
    }


fun main() {
    val a = Flowable.interval(100, TimeUnit.MILLISECONDS)
        .take(10)
        .debounce(300, TimeUnit.MILLISECONDS)
        .map {
            "Debounce $it"
        }

    val b = Flowable.interval(100, TimeUnit.MILLISECONDS)
        .take(10)
        .throttleFirst(300, TimeUnit.MILLISECONDS)
        .map {
            "Throttle $it"
        }

    val c = Flowable.interval(100, TimeUnit.MILLISECONDS)
        .take(10)
        .sample(300, TimeUnit.MILLISECONDS)
        .map {
            "Sample $it"
        }

    Flowable.merge(a, b, c).subscribe({ System.out.println(it) })
    Thread.sleep(1000000)
}