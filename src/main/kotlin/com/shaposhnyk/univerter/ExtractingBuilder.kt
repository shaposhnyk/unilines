package com.shaposhnyk.univerter

import java.util.function.Function
import java.util.function.Predicate

/**
 * Filter builder methods for a ConverterBuilder with a dedicated extractor
 */
interface ExtractingBuilder<T, C, R> : Converter<T, C> {

    /**
     * Creates a new ConverterBuilder, which will process only if extracted value matches the predicate
     */
    fun postFilter(predicate: (R?) -> Boolean): ExtractingBuilder<T, C, R>

    fun jPostFilter(predicate: Predicate<R?>): ExtractingBuilder<T, C, R> {
        return postFilter { r -> predicate.test(r) }
    }

    /**
     * Decorates value extracted by extractor, without changing its type
     */
    fun decorate(fx: (R?) -> R?): ExtractingBuilder<T, C, R>

    fun jDecorate(fx: Function<R, R?>): ExtractingBuilder<T, C, R> {
        return decorate { it -> if (it != null) fx.apply(it) else null }
    }

    fun withExtractionErrorHandler(errorHandler: (Exception, T?) -> R?): ExtractingBuilder<T, C, R>

    fun silenceExtractionErrors(): ExtractingBuilder<T, C, R> {
        return withExtractionErrorHandler { _, _ -> null }
    }
}