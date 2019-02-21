package benchmarks

import examples.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.builders.*
import kotlinx.coroutines.flow.operators.*
import org.openjdk.jmh.annotations.*
import reactor.core.publisher.*
import java.util.concurrent.*

/**
 * Benchmark                     Mode  Cnt    Score    Error  Units
 * FuseBenchmark.fuseableInline  avgt    5  772.411 ± 19.412  us/op
 * FuseBenchmark.regularInline   avgt    5  317.392 ± 14.523  us/op
 */
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class FuseBenchmark {

    private val source = flow {
        repeat(10_000) {
            emit(it)
        }
    }.map { it + 1 }
        .map { it - 1 }
        .filter { true }

    private val fusedInlineSource = flow {
        repeat(10_000) {
            emit(it)
        }
    }.fusedMap() { it + 1 }
        .fusedMap { it - 1 }
        .fusedFilter { true }

    private val flux = Flux.range(0, 10_000).map { it + 1 }
        .map { it - 1 }
        .filter() { true }

    private var state = 0
    private val fluxGenerated = Flux.generate<Int> { sink ->
        sink.next(state)
        if (++state == 10_000) {
            state = 0
            sink.complete()
        }
    }.map { it + 1 }
        .map { it - 1 }
        .filter() { true }

    @Benchmark
    fun flux() = flux.reduce(0) { x1, x2 -> x1 + x2 }.block()

    @Benchmark
    fun fluxGenerator() = fluxGenerated.reduce(0) { x1, x2 -> x1 + x2 }.block()

    @Benchmark
    fun regularInline() = runBlocking {
        source.sum()
    }

    @Benchmark
    fun fuseableInline() = runBlocking {
        fusedInlineSource.sum()
    }
}
