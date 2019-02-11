package reactivestreams

import flow.*
import kotlinx.coroutines.channels.*
import org.reactivestreams.*

/**
 * Transforms the given reactive [Publisher] into [Flow].
 * Backpressure is controlled by [batchSize] parameter that controls the size of in-flight elements
 * and [Subscription.request] size.
 *
 * TODO cancellation is not propagated
 */
public fun <T: Any> Publisher<T>.asFlow(batchSize: Int = 1): Flow<T> = PublisherAsFlow(this, batchSize)

private class PublisherAsFlow<T: Any>(private val publisher: Publisher<T>, private val batchSize: Int) : Flow<T> {

    override suspend fun subscribe(consumer: FlowSubscriber<T>) {
        val channel = Channel<T>(batchSize)
        val subscriber = ReactiveSubscriber(channel, batchSize)
        publisher.subscribe(subscriber)
        var consumed = 0
        for (i in channel) {
            consumer.push(i)
            if (++consumed == batchSize) {
                consumed = 0
                subscriber.subscription.request(batchSize.toLong())
            }
        }
    }

    private class ReactiveSubscriber<T>(private val channel: Channel<T>, private val batchSize: Int) : Subscriber<T> {

        lateinit var subscription: Subscription

        override fun onComplete() {
            channel.close()
        }

        override fun onSubscribe(s: Subscription) {
            subscription = s
            s.request(batchSize.toLong())
        }

        override fun onNext(t: T) {
            require(channel.offer(t))
        }

        override fun onError(t: Throwable?) {
            channel.close(t)
        }
    }
}
