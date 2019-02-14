package kotlinx.coroutines.flow

interface FlowCollector<T> {
    suspend fun emit(value: T)
}