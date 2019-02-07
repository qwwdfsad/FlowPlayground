import flow.*
import flow.operators.*
import flow.source.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.*
import kotlin.coroutines.*

fun <T> Flow<T>.debounce(timeoutMillis: Long): Flow<T> = flow {
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
                    push(lastReceived!!)
                }
                true
            }
        }

        if (lastReceived != null) {
            push(lastReceived!!)
        }

        lastReceived
    }

    flowBridge {
        conflated.send(it)
    }

    // "close with value" idiom
    val lastValue = conflated.poll()
    conflated.close()
    val lastPushedValue = job.await()
    if (lastPushedValue != lastValue && lastValue != null) {
        push(lastValue)
    }
}
