package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*


// TODO exception contract
public suspend fun <T: Any> Flow<T>.awaitSingle(): T {
    var result: T? = null
    flowBridge {
        if (result != null) error("Expected only one element")
        result = it
    }

    return result ?: throw NoSuchElementException("Expected at least one element")
}