package examples

import examples.IntDao.Companion.IO
import examples.IntDao.Companion.Main
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.terminal.*

/**
 * This example shows how to create a Flow-based API for database client,
 * no matter whether it is a suspend-based one or blocking.
 */
interface IntDao {

    companion object {
        @JvmStatic
        val IO = newSingleThreadContext("I/O thread")

        @JvmStatic
        val Main = newSingleThreadContext("Main thread")
    }

    /**
     * Returns flow that emits one int associated with given key.
     *
     * API user should use `flowOn` in order to choose where actual I/O call will be invoked
     */
    fun readInt(key: String): Flow<Int>

    /**
     * Returns flow that emits one int associated with given key.
     *
     * This API is configured to be used with [IO] by default and is a shorthand for: `readInt(key).flowOn(IO)`
     */
    fun readIntWithIoConvention(key: String): Flow<Int>
}

object IntDaoImpl : IntDao {
    override fun readInt(key: String): Flow<Int> = flow {
        println("Doing blocking call in thread: ${Thread.currentThread()}")
        Thread.sleep(100)
        emit(42)
    }

    override fun readIntWithIoConvention(key: String): Flow<Int> = readInt(key).flowOn(IO)
}


suspend fun main() {
    val mainScope = CoroutineScope(Main)
    println("Sample 1: missing 'flowOn' usage:")
    IntDaoImpl.readInt("foo")
        .launchIn(mainScope) {
            onEach {
                println("Received $it on thread ${Thread.currentThread()}")
            }
        }.join()

    println("\nSample 2: with 'flowOn':")
    IntDaoImpl.readInt("foo")
        .flowOn(IO)
        .launchIn(mainScope) {
            onEach {
                println("Received $it on thread ${Thread.currentThread()}")
            }
        }.join()

    println("\nSample 3: with convention:")
    IntDaoImpl.readIntWithIoConvention("foo")
        .launchIn(mainScope) {
            onEach {
                println("Received $it on thread ${Thread.currentThread()}")
            }
        }.join()

    println("\nSample 3: with convention and without launch + join:")
    withContext(Main) {
        IntDaoImpl.readIntWithIoConvention("foo").collect {
            println("Received $it on thread ${Thread.currentThread()}")
        }
    }
}
