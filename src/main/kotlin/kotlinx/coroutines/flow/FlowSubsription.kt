package kotlinx.coroutines.flow

interface FlowSubscriber<T> {
    suspend fun push(value: T)
}