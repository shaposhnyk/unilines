package com.shaposhnyk.unilines.builders

import com.shaposhnyk.unilines.UBiPipeline
import com.shaposhnyk.unilines.UField
import com.shaposhnyk.unilines.UTriConsumer
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

/**
 * Final field based converters
 */
class UCField {

    /**
     * Generic Pipeline, which unites a field and a BiConsumer
     */
    data class Simple<T, C>(val f: UField,
                            val consumer: (T?, C) -> Unit = { _, _ -> Unit })
        : UField by f, UBiPipeline<T, C>, FilteringBuilder<T, C> {
        override fun fields(): List<UBiPipeline<*, *>> = listOf()

        override fun consume(sourceObj: T?, workingCtx: C) {
            try {
                consumer(sourceObj, workingCtx)
            } catch (e: RuntimeException) {
                defaultErrorHandler(f, e)
            }
        }

        /**
         * @return new instance of UBiPipeline with a supplied consumer
         */
        fun <U, Z> withConsumer(newConsumer: (U?, Z) -> Unit): Simple<U, Z> {
            return Simple(f, { t, c -> newConsumer(t, c) })
        }

        /**
         * @return new instance of UBiPipeline with a supplied consumer
         */
        fun <U, Z> withConsumerJ(newConsumer: BiConsumer<U, Z>): Simple<U, Z> {
            return withConsumer { t, c ->
                if (t != null) {
                    newConsumer.accept(t, c)
                }
            }
        }

        /**
         * @return new instance of UBiPipeline with a supplied consumer
         */
        fun <U, Z> withConsumerJF(newConsumer: UTriConsumer<UField, U, Z>): Simple<U, Z> {
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
            return Simple(f, { s, c ->
                if (predicate(s, c)) {
                    this.consume(s, c)
                }
            })
        }
    }

    /**
     * Pipeline, which extracts value from source object,
     * then writes it with a writer dedicated for this particular type
     */
    data class Extracting<T, C, R>(val f: UField,
                                   val extractor: (T?) -> R?,
                                   val writer: (R?, C) -> Unit = { _, _ -> Unit })
        : UField by f, UBiPipeline<T, C>,
            FilteringBuilder<T, C>, ExtractingBuilder<T, C, R> {
        override fun fields(): List<UBiPipeline<*, *>> = listOf()

        override fun extractor(): (T?) -> R? = extractor

        override fun consume(sourceObj: T?, workingCtx: C) {
            try {
                val v1 = extractor(sourceObj)
                writer(v1, workingCtx)
            } catch (e: RuntimeException) {
                defaultErrorHandler(f, e)
            }
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

        fun <Z> withWriterJF(newWriter: UTriConsumer<UField, R, Z>): Extracting<T, Z, R> {
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
     * UBiPipeline, which extracts value from source object, then writes it with a generic writer
     */
    data class UExtracting<T, C, R>(val f: UField,
                                    val extractor: (T?) -> R?,
                                    val writer: (Any?, C) -> Unit = { _, _ -> Unit })
        : UField by f, UBiPipeline<T, C>,
            FilteringBuilder<T, C>, ExtractingBuilder<T, C, R> {
        override fun fields(): List<UBiPipeline<*, *>> = listOf()

        override fun consume(sourceObj: T?, workingCtx: C) {
            try {
                val v1 = extractor(sourceObj)
                writer(v1, workingCtx)
            } catch (e: RuntimeException) {
                defaultErrorHandler(f, e)
            }
        }

        override fun extractor(): (T?) -> R? = extractor

        /*
         * Writer setters
         */
        fun <Z> withWriter(newWriter: (Any?, Z) -> Unit): UExtracting<T, Z, R> {
            return UExtracting(f, extractor, newWriter)
        }

        fun <Z> withWriterF(newWriter: UTriConsumer<UField, Any?, Z>): UExtracting<T, Z, R> {
            return UExtracting(f, extractor, { r, c -> newWriter.accept(f, r, c) })
        }

        fun <Z> withWriterJ(newWriter: BiConsumer<Any, Z>): UExtracting<T, Z, R> {
            return withWriter { r, c -> if (r != null) newWriter.accept(r, c) }
        }

        fun <Z> withWriterJF(newWriter: UTriConsumer<UField, Any, Z>): UExtracting<T, Z, R> {
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
        fun of(f: UField): Simple<Any, Any> {
            return Simple(f)
        }

        /*
         * Converters with value extractor and associated writer.
         * Type of value returned by extractor should match to the type of the writer
         */
        fun <T, C, R> extractingOf(f: UField, fx: Function<T, R?>): Extracting<T, C, R> {
            return Extracting(f, { t -> if (t != null) fx.apply(t) else null })
        }

        /*
         * Converters with value extractor and associated generic writer.
         * Writer accepts Object, it is up to writer to handle all possible input types
         */
        fun <T, C, R> uniExtractingOf(f: UField, fx: Function<T, R?>): UExtracting<T, C, R> {
            return UExtracting(f, { t -> if (t != null) fx.apply(t) else null })
        }

        fun <T, C, R> fUniExtractingOf(f: UField, fx: BiFunction<UField, T, R?>): UExtracting<T, C, R> {
            return UExtracting(f, { t -> if (t != null) fx.apply(f, t) else null })
        }

        fun <T, C> contextMapperOf(f: UField, ctxFx: Consumer<C>): Simple<T, C> {
            return Simple(f, { _, c -> ctxFx.accept(c) })
        }

        /**
         * Default UBiPipeline error handler, which insert given field into stack trace
         */
        fun defaultErrorHandler(f: UField, e: Exception, lineNo: Int = 1) {
            val fileName = "AutoGeneratedUField.java"
            val newStack = StackTraceElement(f::class.java.canonicalName,
                    "from_${f.internalName()}_to_${f.externalName()}.consume", fileName, lineNo)

            val genIdx = e.stackTrace.indexOfFirst { t -> fileName == t.fileName }
            if (genIdx < 0) {
                e.stackTrace = arrayOf(newStack).plus(e.stackTrace)
                throw e
            }
            val before = IntRange(0, genIdx)
            val after = IntRange(genIdx + 1, e.stackTrace.size - 1)
            e.stackTrace = e.stackTrace.sliceArray(before)
                    .plus(arrayOf(newStack))
                    .plus(e.stackTrace.slice(after))
            throw e
        }
    }
}

