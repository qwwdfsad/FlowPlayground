package tck

import junit.framework.Assert.*
import org.junit.*
import org.reactivestreams.*
import org.reactivestreams.tck.*
import flow.*
import java.util.stream.*
import org.reactivestreams.Subscription
import org.reactivestreams.Subscriber
import java.util.ArrayList
import java.util.concurrent.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool.commonPool

class IterableFlowTckTest : PublisherVerification<Long>(TestEnvironment()) {

    private fun generate(num: Long): Array<Long> {
        return LongStream.range(0, if (num >= Integer.MAX_VALUE) 1000000 else num)
            .boxed()
            .toArray { Array(it) { 0L } }
    }

    override fun createPublisher(elements: Long): Publisher<Long> {
        return FlowAsPublisher(generate(elements).asIterable().asFlow())
    }

    override fun createFailedPublisher(): Publisher<Long>? {
        return null
    }

    @Test
    fun testStackOverflowTrampoline() {
        val latch = CountDownLatch(1)
        val collected = ArrayList<Long>()
        val toRequest = 1000L
        val array = generate(toRequest)
        val publisher = FlowAsPublisher(array.asIterable().asFlow())

        publisher.subscribe(object : Subscriber<Long> {
            private lateinit var s: Subscription

            override fun onSubscribe(s: Subscription) {
                this.s = s
                s.request(1)
            }

            override fun onNext(aLong: Long) {
                collected.add(aLong)

                s.request(1)
            }

            override fun onError(t: Throwable) {

            }

            override fun onComplete() {
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)
        assertEquals(collected, array.toList())
    }

    @Test
    fun testConcurrentRequest() {
        val latch = CountDownLatch(1)
        val collected = ArrayList<Long>()
        val n = 50000L
        val array = generate(n)
        val publisher = FlowAsPublisher(array.asIterable().asFlow())

        publisher.subscribe(object : Subscriber<Long> {
            private var s: Subscription? = null

            override fun onSubscribe(s: Subscription) {
                this.s = s
                for (i in 0 until n) {
                    commonPool().execute { s.request(1) }
                }
            }

            override fun onNext(aLong: Long) {
                collected.add(aLong)
            }

            override fun onError(t: Throwable) {

            }

            override fun onComplete() {
                latch.countDown()
            }
        })

        latch.await(50, TimeUnit.SECONDS)
        assertEquals(array.toList(), collected)
    }

    @Ignore
    override fun required_spec309_requestZeroMustSignalIllegalArgumentException() {
        // Uhm
    }

    @Ignore
    override fun required_spec309_requestNegativeNumberMustSignalIllegalArgumentException() {
        // Uhm
    }

    @Ignore
    override fun required_spec109_subscribeThrowNPEOnNullSubscriber() {
        // Type system, yay!
    }
}
