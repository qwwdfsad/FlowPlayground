package kotlinx.coroutines.flow.terminal

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import java.util.ArrayList
import java.util.LinkedHashSet

public suspend fun <T : Any> Flow<T>.toList(destination: MutableList<T> = ArrayList()): List<T> = toCollection(destination)

public suspend fun <T : Any> Flow<T>.toSet(destination: MutableSet<T> = LinkedHashSet()): Set<T> = toCollection(destination)

public suspend fun <T : Any, C : MutableCollection<in T>> Flow<T>.toCollection(destination: C): C {
    flowBridge { value ->
        destination.add(value)
    }
    return destination
}

