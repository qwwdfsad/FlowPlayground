@file:Suppress("unused")

package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlin.coroutines.*

public fun <T : Any> Flow<T>.withUpstreamContext(upstreamContext: CoroutineContext): Flow<T> {
    val job = upstreamContext[Job]
    if (job != null) {
        error("Upstream context operator should not contain job in it, but had $job")
    }

    return flow {
        withContext(upstreamContext) {
            collect {
                emit(it)
            }
        }
    }
}