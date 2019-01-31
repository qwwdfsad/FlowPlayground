package flow

interface FlowSubscription<T> {
    suspend fun push(value: T)
}