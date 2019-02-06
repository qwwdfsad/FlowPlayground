package tck

import kotlinx.coroutines.*
import org.reactivestreams.*
import flow.*
import flow.operators.*
import java.util.concurrent.atomic.*

@Suppress("PublisherImplementation")
class FlowAsPublisher<T>(private val flow: Flow<T>) : Publisher<T> {

    override fun subscribe(subscriber: Subscriber<in T>) {
        subscriber.onSubscribe(FlowSubscription(flow, subscriber))
    }

    private class FlowSubscription<T>(val flow: Flow<T>, val subscriber: Subscriber<in T>) : Subscription {
        @Volatile
        internal var canceled: Boolean = false
        private val requested = AtomicLong(0L)
        private lateinit var producer: CancellableContinuation<Unit>

        // This is actually optimizable
        private var job = GlobalScope.launch(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
            flow.flowBridge { value ->

                if (requested.get() == 0L) {
                    suspendCancellableCoroutine<Unit> {
                        producer = it
                        if (requested.get() != 0L) it.resumeWith(Result.success(Unit))
                    }
                }

                requested.decrementAndGet()

                val result = kotlin.runCatching {
                    subscriber.onNext(value)
                }

                if (result.isFailure) {
                    subscriber.onError(result.exceptionOrNull())
                }
            }

            subscriber.onComplete()
        }

        override fun cancel() {
            canceled = true
            job.cancel()
        }

        @UseExperimental(InternalCoroutinesApi::class)
        override fun request(n: Long) {
            if (n <= 0) {
                return
            }

            if (canceled) return

            job.start()
            var snapshot: Long
            var newValue: Long
            do {
                snapshot = requested.get()
                newValue = snapshot + n
                if (newValue < 0L) newValue = Long.MAX_VALUE

            } while (!requested.compareAndSet(snapshot, newValue))

            // Uhm, obvious candidate to rewriting
            val token = producer.tryResume(Unit)
            if (token != null) {
                producer.completeResume(token)
            }
        }
    }
}
