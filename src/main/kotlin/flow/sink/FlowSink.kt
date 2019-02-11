package flow.sink

import flow.*
import flow.source.*

public interface FlowSink<T : Any> {

}

public fun <T : Any> fromSink(block: (FlowSink<T>) -> Unit): Flow<T> = flow {

}
