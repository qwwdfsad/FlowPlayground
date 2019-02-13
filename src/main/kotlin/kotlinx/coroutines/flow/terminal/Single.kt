package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*

/**
 * Terminal operator, that awaits for one and only one value to be published.
 * Throws [NoSuchElementException] for empty flow and [IllegalStateException] for flow
 * that contains more than one element.
 */
public suspend fun <T: Any> Flow<T>.awaitSingle(): T {
    var result: T? = null
    flowBridge {
        if (result != null) error("Expected only one element")
        result = it
    }

    return result ?: throw NoSuchElementException("Expected at least one element")
}