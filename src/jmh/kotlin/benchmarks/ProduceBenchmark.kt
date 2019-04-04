package benchmarks

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*
import kotlin.test.*

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class ProduceBenchmark {


    @Benchmark
    fun twoCodePaths() = runBlocking {
        val channel = produce<Int> {
            repeat(10_000) {
                if (ThreadLocalRandom.current().nextBoolean()) codePath()
                else codePath2()
            }
        }

        var sum = 0
        for (i in channel) {
            sum += i
        }

        sum
    }

    @Benchmark
    fun oneCodePath() = runBlocking {
        val channel = produce<Int> {
            repeat(10_000) {
                codePath()
            }
        }

        var sum = 0
        for (i in channel) {
            sum += i
        }

        sum
    }



    suspend fun ProducerScope<Int>.codePath() {
        codePathNested()
        assertTrue(true)
    }

    suspend fun ProducerScope<Int>.codePathNested() {
        codePathNested(ThreadLocalRandom.current().nextInt())
        assertTrue(true)
    }

    suspend fun ProducerScope<Int>.codePathNested(x: Int) {
        send(x)
        assertTrue(true)
    }

    suspend fun ProducerScope<Int>.codePath2() {
        codePathNested()
        assertTrue(true)
    }

    suspend fun ProducerScope<Int>.codePathNested2() {
        codePathNested(ThreadLocalRandom.current().nextInt())
        assertTrue(true)
    }

    suspend fun ProducerScope<Int>.codePathNested2(x: Int) {
        send(x)
        assertTrue(true)
    }
}
