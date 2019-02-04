package flow

interface FlowSubscriber<T> {
    suspend fun push(value: T)
}