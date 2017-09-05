package com.shaposhnyk.univerter

import java.util.function.Predicate

/**
 * A field converter, which consumes a source object (T) and a working context (C)
 */
interface PostFilteringBuilder<T, C, R> : Field {
    fun postFilter(predicate: (R?) -> Boolean): Builders.Extracting<T, C, R>

    fun jPostFilter(predicate: Predicate<R?>): Builders.Extracting<T, C, R> {
        return postFilter { r -> predicate.test(r) }
    }
}