package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*

fun <T: Any> Iterable<Flow<T>>.merge(): Flow<T> = asFlow().flatMap { it }

suspend fun main() {
    val f1 = flowOf(1, 2, 3, 4, 5).delayEach(100)
    val f2 = flowOf(6, 7, 8, 9, 10).delayEach(75)
    listOf(f1, f2).merge().collect {
        println(it)
    }
}