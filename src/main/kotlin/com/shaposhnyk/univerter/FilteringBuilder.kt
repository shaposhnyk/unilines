package com.shaposhnyk.univerter

import java.util.function.Predicate

/**
 * A field converter, which consumes a source object (T) and a working context (C)
 */
interface FilteringBuilder<T, C> : Converter<T, C> {

    /**
     * Creating a new ConverterBuilder which processes only matching input & contexts
     */
    fun filter(predicate: (T?, C) -> Boolean): FilteringBuilder<T, C>

    fun filterS(predicate: (T?) -> Boolean): FilteringBuilder<T, C> {
        return filter { s: T?, _: C -> predicate(s) }
    }

    fun jFilterS(p: Predicate<T?>): FilteringBuilder<T, C> {
        return filterS { it: T? -> p.test(it) }
    }

    fun withErrorHandler(errorHandler: (Exception, T?, C) -> Unit): FilteringBuilder<T, C>

    fun silenceErrors(): FilteringBuilder<T, C> {
        return withErrorHandler { _, _, _ -> Unit }
    }
}