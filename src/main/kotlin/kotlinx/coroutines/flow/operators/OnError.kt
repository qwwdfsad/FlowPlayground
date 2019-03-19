package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

// TODO discuss usability from IDE with lambda as last parameter
public typealias ExceptionPredicate = (Throwable) -> Boolean

private val ALWAYS_TRUE: ExceptionPredicate = { true }

/**
 * Switches to the [fallback] flow if the original flow throws an exception that matches the [predicate].
 *
 * TODO prefix naming
 */
public fun <T : Any> Flow<T>.onErrorCollect(
    fallback: Flow<T>,
    predicate: ExceptionPredicate = ALWAYS_TRUE
): Flow<T> = collectSafely { e ->
    if (!predicate(e)) throw e
    fallback.collect {
        emit(it)
    }
}

/**
 * Emits the [fallback] value and finishes successfully if the original flow throws exception that matches the given [predicate];
 */
public fun <T : Any> Flow<T>.onErrorReturn(fallback: T, predicate: ExceptionPredicate = ALWAYS_TRUE): Flow<T> =
    collectSafely { e ->
        if (!predicate(e)) throw e
        emit(fallback)
    }

/**
 * Operator that retries to collect the given flow in an exception that matches the given [predicate] occurs.
 */
public fun <T : Any> Flow<T>.retry(
    retries: Int = Int.MAX_VALUE,
    predicate: ExceptionPredicate = ALWAYS_TRUE
): Flow<T> {
    require(retries > 0) { "Expected positive amount of retries, but had $retries" }
    return flow {
        var retries = retries
        // Note that exception may come from the downstream operators, we should not switch on that
        while (true) {
            var fromDownstream = false
            try {
                collect { value ->
                    try {
                        emit(value)
                    } catch (e: Throwable) {
                        fromDownstream = predicate(e)
                        throw e
                    }
                }
                break
            } catch (e: Throwable) {
                if (fromDownstream) throw e
                if (!predicate(e) || retries-- == 0) throw e
            }
        }
    }
}

private inline fun <T : Any> Flow<T>.collectSafely(crossinline onException: suspend FlowCollector<T>.(Throwable) -> Unit): Flow<T> =
    flow {
        // Note that exception may come from the downstream operators, we should not switch on that
        var fromDownstream = false
        try {
            collect {
                try {
                    emit(it)
                } catch (e: Throwable) {
                    fromDownstream = true
                    throw e
                }
            }
        } catch (e: Throwable) {
            if (fromDownstream) throw e
            onException(e)
        }
    }

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Flow analogue is named onErrorCollect",
    replaceWith = ReplaceWith("onErrorCollect(fallback)")
)
public fun <T : Any> Flow<T>.onErrorResume(fallback: Flow<T>): Flow<T> = error("Should not be called")
