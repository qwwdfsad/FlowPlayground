package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*


public fun <T : Any> Flow<T>.onErrorResumeWith(flow: Flow<T>): Flow<T> = flow {
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
        flow.collect {
            emit(it)
        }
    }
}

public fun <T : Any> Flow<T>.onErrorReturn(fallback: T) = flow {
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
        emit(fallback)
    }
}