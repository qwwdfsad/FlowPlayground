package kotlinx.coroutines.flow


/**
 * A cold asynchronous stream of the data, that emits from zero to N (where N can be unbounded)
 * values and completes normally or with an exception.
 *
 * All transformations on the flow, such as [map] and [filter] (or any other operator from
 * `kotlinx.coroutines.flow.operators`) do not trigger flow collection or execution, only
 * terminal operators do trigger it.
 *
 * Flow can be collected **synchronously** (in a suspending manner, without actual blocking)
 * using [collect] extension that will complete normally or exceptionally:
 * ```
 * try {
 *   flow.collect { value ->
 *     println("Received $value")
 *   }
 * } catch (e: Exception) {
 *    println("Flow has thrown an exception: $e")
 * }
 * ```
 * Additionally, the library provides a rich set of terminal operators in `kotlinx.coroutines.flow.terminal`, such as
 * [single], [first], [reduce] etc.
 *
 * Flow also can be collected asynchronously using launch-like coroutine:
 * ```
 * flow.launchIn(uiScope) {
 *   onEach { value ->
 *     println("Received $value")
 *   }
 *
 *   catch<MyException> {
 *     println("Flow has failed")
 *   }
 *
 *   finally {
 *     println("Doing cleanup)
 *   }
 * }
 * ```
 *
 * Flow does not carry information whether it is a cold stream (that can be collected multiple times and
 * triggers its evaluation every time collection is executed) or hot one, but conventionally flow represents a cold stream.
 *
 * Flow is a **pure** concept: it encapsulates its own execution context and never propagates it to the downstream, thus making
 * reasoning about execution context of particular transformations or terminal operations trivial.
 * There are two ways of changing flow's context: [flowOn][Flow.flowOn] and [flowWith][Flow.flowWith].
 * The former changes the upstream context ("everything above flowOn operator") while the latter
 * changes the context of the flow in flowWith body. For additional information refer to these operators documentation.
 *
 * This reasoning can be demonstrated in the practice:
 * ```
 * val flow = flowOf(1, 2, 3)
 *   .map { it + 1 } // Will be executed in ctx_1
 *   .flowOn(ctx_1) // Changes upstream context: flowOf and map
 *
 * // Now we have flow that is pure: it is executed somewhere
 * // but this information is encapsulated in it
 *
 * val filtered = flow
 *  .filter { it == 3 } // Pure operator without a context
 *
 * withContext(Dispatchers.Main) {
 *    // All not encapsulated operators will be executed in Main: filter and single
 *    val result = filtered.single()
 *    myUi.text = result
 * }
 * ```
 *
 * Flow is also Reactive Streams compliant, you can safely interop it with reactive streams using [Flow.asPublisher] and [Publisher.asFlow]
 */
public interface Flow<T : Any> {

    /**
     * Accepts the collector and [emits][FlowCollector.emit] values into it.
     * A proper implementation of this method has the following constraints:
     * 1) It should not change a coroutine context (e.g. with `withContext(Dispatchers.IO)`) when emitting values.
     *
     * Only coroutine builders that inherit the context are allowed, for example the following code is allowed:
     * ```
     * coroutineScope { // Context is inherited
     *     launch { // Dispatcher is not overridden
     *        collector.emit(someValue)
     *     }
     * }
     * ```
     *
     * 2) It should serialize serialize calls to [emit][FlowCollector.emit] as [FlowCollector] implementations are not thread safe.
     */
    public suspend fun collect(collector: FlowCollector<T>)
}
