package flow

fun <T> Iterable<Flow<T>>.merge(): Flow<T> = asFlow().flatMap { it }

suspend fun main() {
    val f1 = flow(1, 2, 3, 4, 5).delayEach(100)
    val f2 = flow(6, 7, 8, 9, 10).delayEach(75)
    listOf(f1, f2).merge().consumeEach {
        println(it)
    }
}