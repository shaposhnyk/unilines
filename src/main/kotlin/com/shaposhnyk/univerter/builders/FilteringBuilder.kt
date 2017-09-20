package com.shaposhnyk.univerter.builders

import com.shaposhnyk.univerter.UBiPipeline
import java.util.function.Predicate

/**
 * A field converter, which consumes a source object (T) and a working context (C)
 */
interface FilteringBuilder<T, C> : UBiPipeline<T, C> {

    /**
     * @return new ConverterBuilder which processes only input & context matching the predicate
     */
    fun filter(predicate: (T?, C) -> Boolean): FilteringBuilder<T, C>

    fun filterS(predicate: (T?) -> Boolean): FilteringBuilder<T, C> {
        return filter { s: T?, _: C -> predicate(s) }
    }

    fun filterJS(p: Predicate<T?>): FilteringBuilder<T, C> {
        return filterS { it: T? -> p.test(it) }
    }

    /**
     * @return new ConverterBuilder with a given exception handler
     */
    fun withErrorHandler(errorHandler: (Exception, T?, C) -> Unit): FilteringBuilder<T, C>

    /**
     * @return new ConverterBuilder which silently swallows exceptions
     */
    fun silenceErrors(): FilteringBuilder<T, C> {
        return withErrorHandler { _, _, _ -> Unit }
    }
}