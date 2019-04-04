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
 * FuseBenchmark.flux            avgt    5  156.115 ±  7.660  us/op
 * FuseBenchmark.fluxGenerator   avgt    5  183.943 ±  3.528  us/op
 * FuseBenchmark.fuseableInline  avgt    5  534.541 ± 14.829  us/op
 * FuseBenchmark.regularInline   avgt    5  294.328 ± 10.653  us/op
 */
//@Warmup(iterations = 5, time = 1)
//@Measurement(iterations = 5, time = 1)
//@BenchmarkMode(Mode.AverageTime)
//@OutputTimeUnit(TimeUnit.MICROSECONDS)
//@State(Scope.Benchmark)
//@Fork(1)
open class FuseBenchmark {

//    private val source = (0..10_000).asFlow()
//        .map { it + 1 }
//        .map { it - 1 }
//        .filter { true }
//
//    private val fusedInlineSource = (0..10_000).asFlow()
//        .fusedMap { it + 1 }
//        .fusedMap { it - 1 }
//        .fusedFilter { true }
//
//    private val flux = Flux.range(0, 10_000)
//        .map { it + 1 }
//        .map { it - 1 }
//        .filter { true }
//
//    private var state = 0
//    private val fluxGenerated = Flux.generate<Int> { sink ->
//        sink.next(state)
//        if (++state == 10_000) {
//            state = 0
//            sink.complete()
//        }
//    }.map { it + 1 }
//        .map { it - 1 }
//        .filter { true }
//
//    @Benchmark
//    fun flux() = flux.reduce(0) { x1, x2 -> x1 + x2 }.block()
//
//    @Benchmark
//    fun fluxGenerator() = fluxGenerated.reduce(0) { x1, x2 -> x1 + x2 }.block()
//
//    @Benchmark
//    fun regularInline() = runBlocking {
//        source.sum()
//    }
//
//    @Benchmark
//    fun fuseableInline() = runBlocking {
//        fusedInlineSource.sum()
//    }
}
