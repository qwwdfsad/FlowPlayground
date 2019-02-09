package reactivestreams.original.examples

import org.reactivestreams.example.unicast.AsyncIterablePublisher
import org.reactivestreams.Publisher
import org.reactivestreams.example.unicast.NumberIterablePublisher
import org.reactivestreams.tck.TestEnvironment
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import org.reactivestreams.tck.PublisherVerification
import org.testng.annotations.*
import reactivestreams.*

@Test // Must be here for TestNG to find and run this, do not remove
class IterablePublisherTest : PublisherVerification<Int>(TestEnvironment()) {

    private var e: ExecutorService? = null
    @BeforeClass
    internal fun before() {
        e = Executors.newFixedThreadPool(4)
    }

    @AfterClass
    internal fun after() {
        if (e != null) e!!.shutdown()
    }

    override fun createPublisher(elements: Long): Publisher<Int> {
        assert(elements <= maxElementsFromPublisher())
        return NumberIterablePublisher(0, elements.toInt(), e!!)
    }

    override fun createFailedPublisher(): Publisher<Int>? {
        return null
    }

    override fun maxElementsFromPublisher(): Long {
        return Integer.MAX_VALUE.toLong()
    }
}