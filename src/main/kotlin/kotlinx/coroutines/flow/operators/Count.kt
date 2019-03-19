package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

public fun <T : Any> Flow<T>.count(): Flow<Long> = flow {
    var i = 0L
    collect {
        ++i
    }
    emit(i)
}
