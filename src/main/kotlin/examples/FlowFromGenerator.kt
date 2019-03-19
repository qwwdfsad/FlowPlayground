package examples

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

fun fibonacci(): Flow<Long> = flow {
    emit(1L)
    var f1 = 1L
    var f2 = 1L
    while (true) {
        var tmp = f1
        f1 = f2
        f2 += tmp
        emit(f1)
    }
}

suspend fun main() {
    fibonacci()
        .limit(10)
        .skip(5).collect {
            println(it)
        }
}