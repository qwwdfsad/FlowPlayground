package flow


/**
 * Base reactive class that represent cold asynchronous stream of the data, that can be transformed and consumed.
 *
 */
interface Flow<T> {
    suspend fun subscribe(consumer: FlowSubscriber<T>)
}