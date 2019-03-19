package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

/**
 * Merges given sequence of flows into a single flow with no guarantees on the order.
 *
 * TODO this is the least tested and documented place because it is trivially expressed via flatMap
 */
public fun <T: Any> Iterable<Flow<T>>.merge(): Flow<T> = asFlow().flatMap { it }
