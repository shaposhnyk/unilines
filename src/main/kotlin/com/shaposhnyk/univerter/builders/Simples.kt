package com.shaposhnyk.univerter.builders

import com.shaposhnyk.univerter.Converter
import com.shaposhnyk.univerter.Field
import com.shaposhnyk.univerter.TriConsumer
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function

/**
 * Builder for final field converting consumers
 * Created by vlad on 25.08.17.
 */
class Simples {

    /**
     * Generic converter, which wraps field and a BiConsumer
     */
    data class Simple<T, C>(val f: Field,
                            val consumer: (T?, C) -> Unit = { _, _ -> Unit })
        : Field by f, Converter<T, C>, FilteringBuilder<T, C> {
        override fun fields(): List<Converter<*, *>> = listOf()

        override fun consume(sourceObj: T?, workingCtx: C) {
            consumer(sourceObj, workingCtx)
        }

        fun <U, Z> withConsumer(newConsumer: (U?, Z) -> Unit): Simple<U, Z> {
            return Simple(f, { t, c -> newConsumer(t, c) })
        }

        fun <U, Z> withConsumerJ(newConsumer: BiConsumer<U, Z>): Simple<U, Z> {
            return withConsumer { t, c -> if (t != null) newConsumer.accept(t, c) }
        }

        fun <U, Z> withConsumerJF(newConsumer: TriConsumer<Field, U, Z>): Simple<U, Z> {
            return withConsumer { t, c -> if (t != null) newConsumer.accept(f, t, c) }
        }

        override fun withErrorHandler(errorHandler: (Exception, T?, C) -> Unit): Simple<T, C> {
            return Simple(f, { t, c ->
                try {
                    consumer(t, c)
                } catch (e: Exception) {
                    errorHandler(e, t, c)
                }
            })
        }

        override fun filter(predicate: (T?, C) -> Boolean): Simple<T, C> {
            return Simple(f, { s, c -> if (predicate(s, c)) this.consume(s, c) })
        }
    }

    /**
     * Converter, which extracts value from source object, then writes it with a dedicated writer
     */
    data class Extracting<T, C, R>(val f: Field,
                                   val extractor: (T?) -> R?,
                                   val writer: (R?, C) -> Unit = { _, _ -> Unit })
        : Field by f, Converter<T, C>,
            FilteringBuilder<T, C>, ExtractingBuilder<T, C, R> {
        override fun fields(): List<Converter<*, *>> = listOf()

        override fun consume(sourceObj: T?, workingCtx: C) {
            val v1 = extractor(sourceObj)
            writer(v1, workingCtx)
        }

        /*
         * Writers
         */

        fun <Z> withWriter(newWriter: (R?, Z) -> Unit): Extracting<T, Z, R> {
            return Extracting(f, extractor, newWriter)
        }

        fun <Z> withWriterJ(newWriter: BiConsumer<R, Z>): Extracting<T, Z, R> {
            return withWriter { r, c -> if (r != null) newWriter.accept(r, c) }
        }

        fun <Z> withWriterJF(newWriter: TriConsumer<Field, R, Z>): Extracting<T, Z, R> {
            return withWriter { r, c -> if (r != null) newWriter.accept(f, r, c) }
        }

        /*
         * Filters
         */
        override fun filter(predicate: (T?, C) -> Boolean): Simple<T, C> {
            return Simple(f, { t, c -> if (predicate(t, c)) this.consume(t, c) })
        }

        override fun withErrorHandler(errorHandler: (Exception, T?, C) -> Unit): Simple<T, C> {
            return Simple<T, C>(f, { t: T?, c: C -> this.consume(t, c) })
                    .withErrorHandler(errorHandler)
        }

        override fun withExtractionErrorHandler(errorHandler: (Exception, T?) -> R?): Extracting<T, C, R> {
            return Extracting<T, C, R>(f, { t ->
                try {
                    extractor(t)
                } catch (e: Exception) {
                    errorHandler(e, t)
                }
            }, writer)
        }

        override fun postFilter(predicate: (R?) -> Boolean): Extracting<T, C, R> {
            return Extracting(f, extractor, { r, c -> if (predicate(r)) this.writer(r, c) else Unit })
        }

        override fun decorate(fx: (R?) -> R?): Extracting<T, C, R> {
            return Extracting(f, { fx(extractor(it)) }, this.writer)
        }
    }


    /**
     * Converter, which extracts value from source object, then writes it with a dedicated writer
     */
    data class UExtracting<T, C, R>(val f: Field,
                                    val extractor: (T?) -> R?,
                                    val writer: (Any?, C) -> Unit = { _, _ -> Unit })
        : Field by f, Converter<T, C>,
            FilteringBuilder<T, C>, ExtractingBuilder<T, C, R> {
        override fun fields(): List<Converter<*, *>> = listOf()

        override fun consume(sourceObj: T?, workingCtx: C) {
            val v1 = extractor(sourceObj)
            writer(v1, workingCtx)
        }

        /*
         * Writer setters
         */
        fun <Z> withWriter(newWriter: (Any?, Z) -> Unit): UExtracting<T, Z, R> {
            return UExtracting(f, extractor, newWriter)
        }

        fun <Z> withWriterJ(newWriter: BiConsumer<Any, Z>): UExtracting<T, Z, R> {
            return withWriter { r, c -> if (r != null) newWriter.accept(r, c) }
        }

        fun <Z> withWriterJF(newWriter: TriConsumer<Field, Any, Z>): UExtracting<T, Z, R> {
            return withWriter { r, c -> if (r != null) newWriter.accept(f, r, c) }
        }

        /*
         * Conditions
         */
        override fun filter(predicate: (T?, C) -> Boolean): Simple<T, C> {
            return Simple(f, { t, c -> if (predicate(t, c)) this.consume(t, c) })
        }

        override fun withErrorHandler(errorHandler: (Exception, T?, C) -> Unit): Simple<T, C> {
            return Simple<T, C>(f, { t: T?, c: C -> this.consume(t, c) })
                    .withErrorHandler(errorHandler)
        }

        override fun postFilter(predicate: (R?) -> Boolean): Extracting<T, C, R> {
            return Extracting(f, extractor, { t: R?, c: C -> if (predicate(t)) writer(t, c) })
        }

        /*
         * Value decorators
         */
        override fun decorate(fx: (R?) -> R?): UExtracting<T, C, R> {
            return UExtracting(f, { fx(extractor(it)) }, writer)
        }

        override fun decorateJ(fx: Function<R, R?>): UExtracting<T, C, R> {
            return decorate { it -> if (it != null) fx.apply(it) else null }
        }

        override fun withExtractionErrorHandler(errorHandler: (Exception, T?) -> R?): UExtracting<T, C, R> {
            return UExtracting<T, C, R>(f, { t ->
                try {
                    extractor(t)
                } catch (e: Exception) {
                    errorHandler(e, t)
                }
            }, writer)
        }

        fun <U> map(fx: (R?) -> U?): UExtracting<T, C, U> {
            return UExtracting(f, { it: T? -> fx(extractor(it)) }, writer)
        }

        fun <U> mapJ(fx: Function<R, U?>): UExtracting<T, C, U> {
            return map { if (it != null) fx.apply(it) else null }
        }
    }

    companion object Builder {
        /**
         * @return most generic convertor which is a function of source object (T) and working context (U)
         */
        fun <T, U> of(f: Field): Simple<Any, Any> {
            return Simple(f)
        }

        /*
         * Convertors with value extractor and associated writer.
         * Type of value returned by extractor should match to the type of the writer
         */
        fun <T, C, R> extractingOf(f: Field, fx: Function<T, R?>): Extracting<T, C, R> {
            return Extracting(f, { t -> if (t != null) fx.apply(t) else null })
        }

        /*
         * Convertors with value extractor and associated generic writer.
         * Writer accepts Object, it is up to writer to handle all possible input types
         */

        fun <T, C, R> uniExtractingOf(f: Field, fx: Function<T, R?>): UExtracting<T, C, R> {
            return UExtracting(f, { t -> if (t != null) fx.apply(t) else null })
        }

        fun <T, C, R> fUniExtractingOf(f: Field, fx: BiFunction<Field, T, R?>): UExtracting<T, C, R> {
            return UExtracting(f, { t -> if (t != null) fx.apply(f, t) else null })
        }
    }
}

