package kotlinx.coroutines.flow


/**
 * Base reactive class that represent cold asynchronous stream of the data, that can be transformed and consumed.
 */
interface Flow<T : Any> {
    suspend fun subscribe(consumer: FlowSubscriber<T>)
}