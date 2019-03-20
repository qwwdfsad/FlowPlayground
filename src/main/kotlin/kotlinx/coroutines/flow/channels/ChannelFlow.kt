package kotlinx.coroutines.flow.channels

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

/**
 * Represents given broadcast channel as a hot flow.
 * Every flow consumer will trigger new broadcast channel subscription.
 *
 * ### Cancellation semantics
 * 1) Flow consumer is cancelled when the original channel is cancelled
 * 2) Flow consumer completes normally when the original channel completes (is closed) normally
 * 3) If the flow consumer fails with an exception, subscription
 */
public fun <T : Any> BroadcastChannel<T>.asFlow(): Flow<T> = flow {
    val subscription = openSubscription()
    // TODO merge with coroutines 1.2.0 and use 'consumeEach'
    try {
        for (element in subscription) {
            emit(element)
        }
    } finally {
        subscription.cancel()
    }
}

/**
 * Creates a [broadcast] coroutine that collects the given flow.
 *
 * This transformation is **stateful**, it launches a [broadcast] coroutine
 * that collects the given flow and thus resulting channel should be properly closed or cancelled.
 */
public fun <T : Any> Flow<T>.broadcastIn(
    scope: CoroutineScope, capacity: Int = 1,
    start: CoroutineStart = CoroutineStart.LAZY
): BroadcastChannel<T> = scope.broadcast(capacity = capacity, start = start) {
    collect { value ->
        send(value)
    }
}

/**
 * Creates a [produce] coroutine that collects the given flow.
 *
 * This transformation is **stateful**, it launches a [produce] coroutine
 * that collects the given flow and thus resulting channel should be properly closed or cancelled.
 */
public fun <T : Any> Flow<T>.produceIn(
    scope: CoroutineScope,
    capacity: Int = 1
): ReceiveChannel<T> = scope.produce(capacity = capacity) {
    // TODO it would be nice to have it lazy as well
    collect { value ->
        send(value)
    }
}

@Deprecated(message = "Use BroadcastChannel.asFlow()", level = DeprecationLevel.ERROR)
fun BehaviourSubject(): Any = error("Should not be called")

@Deprecated(
    message = "ReplaySubject is not supported. the closest analogue is buffered broadcast channel",
    level = DeprecationLevel.ERROR
)
fun ReplaySubject(): Any = error("Should not be called")

@Deprecated(message = "PublishSubject is not supported", level = DeprecationLevel.ERROR)
fun PublishSubject(): Any = error("Should not be called")

