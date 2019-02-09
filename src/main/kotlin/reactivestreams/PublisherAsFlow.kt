package reactivestreams

import flow.*
import flow.operators.*
import io.reactivex.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.reactivestreams.*
import kotlin.coroutines.*

fun <T> Publisher<T>.asFlow(): Flow<T> = PublisherAsFlow(this)
var i = 0
private class PublisherAsFlow<T>(private val publisher: Publisher<T>) : Flow<T> {

    override suspend fun subscribe(consumer: FlowSubscriber<T>) {
        val channel = Channel<T>(1)
        val subscriber = ReactiveSubscriber(channel)
        publisher.subscribe(subscriber)
        for (i in channel) {
            consumer.push(i)
            subscriber.subscription.request(1)
        }
    }

    private class ReactiveSubscriber<T>(private val channel: Channel<T>) : Subscriber<T> {

        lateinit var subscription: Subscription

        override fun onComplete() {
            channel.close()
        }

        override fun onSubscribe(s: Subscription) {
            subscription = s
            s.request(1)
        }

        override fun onNext(t: T) {
            require(channel.offer(t))
        }

        override fun onError(t: Throwable?) {
            channel.close(t)
        }
    }
}
