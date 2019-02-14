package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.selects.*
import kotlin.coroutines.*

fun <T : Any> Flow<T>.debounce(timeoutMillis: Long): Flow<T> =
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
