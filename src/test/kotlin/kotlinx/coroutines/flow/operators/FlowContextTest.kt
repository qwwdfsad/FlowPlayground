package kotlinx.coroutines.flow.operators

import kotlinx.coroutines.flow.*
import kotlin.coroutines.*

interface Foo {
    var x: Int

    fun foo() {

    }
}


class FooWrapper(foo: Foo) : Foo by foo {

}

