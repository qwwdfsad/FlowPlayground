package examples

import examples.IntDao.Companion.IO
import examples.IntDao.Companion.Main
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.operators.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.terminal.*

/**
 * This example shows how to create a Flow-based API for database client,
 * no matter whether it is a suspend-based one or blocking.
 *
 * Note that this example exists mostly for compatibility purpose and we do not recommend
 * to expose database API in the form of flows instead of regular suspend functions.
 *
 * Example of such "real" usage may be found in Android samples: https://github.com/googlesamples/android-architecture-components/blob/master/BasicRxJavaSample/app/src/main/java/com/example/android/observability/ui/UserActivity.java
 * ```
 * mDisposable.add(mViewModel.getUserName()
 *  .subscribeOn(Schedulers.io())
 *  .observeOn(AndroidSchedulers.mainThread())
 *  .subscribe(userName -> mUserName.setText(userName),
 *       throwable -> Log.e(TAG, "Unable to update username", throwable)));
 * ```
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
     * API user should use `withUpstreamContext` in order to choose
     * where API will be invoked
     */
    fun readInt(key: String): Flow<Int>

    /**
     * Returns flow that emits one int associated with given key.
     *
     * This API is configured to be used with [IO] by default and is a shorthand for:
     * `readInt(key).withUpstreamContext(IO)]
     */
    fun readIntWithIoConvention(key: String): Flow<Int>
}

object IntDaoImpl : IntDao {
    override fun readInt(key: String): Flow<Int> = flow {
        println("Doing blocking call in thread: ${Thread.currentThread()}")
        Thread.sleep(100)
        push(42)
    }

    override fun readIntWithIoConvention(key: String): Flow<Int> =
        flow(IO) {
            println("Doing blocking call in thread: ${Thread.currentThread()}")
            Thread.sleep(100)
            push(42)
        }
}


suspend fun main() {
    println("Sample 1: missing 'withUpstreamContext' usage:")
    IntDaoImpl.readInt("foo")
        .consumeOn(Main) {
            println("Received $it on thread ${Thread.currentThread()}")
        }.join()

    println("\nSample 2: with 'withUpstreamContext':")
    IntDaoImpl.readInt("foo")
        .withUpstreamContext(IO)
        .consumeOn(Main) {
            println("Received $it on thread ${Thread.currentThread()}")
        }.join()

    println("\nSample 3: with convention:")
    IntDaoImpl.readIntWithIoConvention("foo")
        .consumeOn(Main) {
            println("Received $it on thread ${Thread.currentThread()}")
        }.join()
}
