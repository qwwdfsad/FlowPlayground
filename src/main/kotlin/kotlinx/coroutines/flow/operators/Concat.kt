package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*

fun <T: Any> Flow<Flow<T>>.concat(): Flow<T> =
    flow {
        collect {
            val inner = it
            inner.collect { value ->
                emit(value)
            }
        }
    }

suspend fun main() {
    val f1 = flow(1, 2, 3).delayEach(1000)
    val f2 = flow(1, 2, 3).delayEach(1000).map { 3 + it }
    flow(f1, f2).concat().collect {
        println(it)
    }
}