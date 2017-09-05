package com.shaposhnyk.univerter

import java.util.function.Predicate

/**
 * A field converter, which consumes a source object (T) and a working context (C)
 */
interface FilteringBuilder<T, C> : Field {
    fun filter(predicate: (T?, C) -> Boolean): Builders.Simple<T, C>

    fun filter(predicate: (T?) -> Boolean): Builders.Simple<T, C> {
        return filter { s: T?, _: C -> predicate(s) }
    }

    fun jFilter(p: Predicate<T?>): Builders.Simple<T, C> {
        return filter { it: T? -> p.test(it) }
    }
}