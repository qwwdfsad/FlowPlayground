package flow.terminal

interface FlowConsumer<T : Any> {

    suspend fun onNext(element: T)

    suspend fun onError(throwable: Throwable)

    suspend fun onComplete()
}