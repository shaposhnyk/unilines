package com.shaposhnyk.univerter

import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate

/**
 * Builder for final field converting consumers
 * Created by vlad on 25.08.17.
 */
class Builders {

    /**
     * Generic convertor, which wraps field and a BiConsumer
     */
    data class Simple<T, C>(val f: Field,
                            val consumer: (T, C) -> Unit = { _, _ -> Unit })
        : Field by f, Converter<T, C> {
        override fun fields(): List<Converter<*, *>> = listOf()

        override fun consume(source: T, ctx: C) {
            consumer(source, ctx)
        }

        fun withCondition(predicate: (T) -> Boolean): Simple<T, C> {
            return Simple(f, { s, c -> if (predicate(s)) this.consume(s, c) else Unit })
        }

        fun withCondition(predicate: (T, C) -> Boolean): Simple<T, C> {
            return Simple(f, { s, c -> if (predicate(s, c)) this.consume(s, c) else Unit })
        }
    }

    /**
     * Converter, which extracts value from source object, then writes it with a dedicated writer
     */
    data class Extracting<T, C, R>(val f: Field,
                                   val extractor: (T) -> R?,
                                   val condition: (R?) -> Boolean = { true },
                                   val writer: (R?, C) -> Unit = { _, _ -> Unit })
        : Field by f, Converter<T, C> {
        override fun fields(): List<Converter<*, *>> = listOf()

        override fun consume(source: T, ctx: C) {
            val v1 = extractor(source)
            if (condition(v1)) writer(v1, ctx)
        }

        fun <Z> withWriter(newWriter: (R?, Z) -> Unit): Extracting<T, Z, R> {
            return Extracting(f, extractor, condition, { r, c -> if (r != null) newWriter(r, c) })
        }

        fun <Z> withNullWriter(newWriter: (R?, Z) -> Unit): Extracting<T, Z, R> {
            return Extracting(f, extractor, condition, newWriter)
        }

        fun <Z> withJWriter(newWriter: BiConsumer<R?, Z>): Extracting<T, Z, R> {
            return withWriter { r, c -> newWriter.accept(r, c) }
        }

        fun withCondition(predicate: (R?) -> Boolean): Extracting<T, C, R> {
            return Extracting(f, extractor, condition, { r, c -> if (predicate(r)) this.writer(r, c) else Unit })
        }

        fun withJCondition(predicate: Predicate<R?>): Extracting<T, C, R> {
            return withCondition { r -> predicate.test(r) }
        }

        fun withDecorator(fx: (R) -> R): Extracting<T, C, R> {
            return Extracting(f, { s: T ->
                val v1 = this.extractor(s)
                if (v1 != null) fx(v1) else null
            }, condition, this.writer)
        }

        fun withNullDecorator(fx: (R?) -> R?): Extracting<T, C, R> {
            return Extracting(f, { s: T -> fx(this.extractor(s)) }, condition, this.writer)
        }

        fun withJDecorator(fx: Function<R, R>): Extracting<T, C, R> {
            return withDecorator { r -> fx.apply(r) }
        }

        fun withJNullDecorator(fx: Function<R?, R?>): Extracting<T, C, R> {
            return withNullDecorator { r -> fx.apply(r) }
        }
    }


    /**
     * Converter, which extracts value from source object, then writes it with a dedicated writer
     */
    data class UExtracting<T, C, R>(val f: Field,
                                    val extractor: (T) -> R?,
                                    val condition: (R?) -> Boolean = { true },
                                    val writer: (Any?, C) -> Unit = { _, _ -> Unit })
        : Field by f, Converter<T, C> {
        override fun fields(): List<Converter<*, *>> = listOf()

        override fun consume(source: T, ctx: C) {
            val v1 = extractor(source)
            if (condition(v1)) writer(v1, ctx)
        }

        /*
         * Writer setters
         */
        fun <Z> withWriter(newWriter: (Any, Z) -> Unit): UExtracting<T, Z, R> {
            return UExtracting(f, extractor, condition, { r, c -> if (r != null) newWriter(r, c) })
        }

        fun <Z> withFWriter(newWriter: (Field, Any, Z) -> Unit): UExtracting<T, Z, R> {
            return UExtracting(f, extractor, condition, { r, c -> if (r != null) newWriter(f, r, c) })
        }

        fun <Z> withNullWriter(newWriter: (Any?, Z) -> Unit): UExtracting<T, Z, R> {
            return UExtracting(f, extractor, condition, { r, c -> newWriter(r, c) })
        }

        fun <Z> withFNullWriter(newWriter: (Field, Any?, Z) -> Unit): UExtracting<T, Z, R> {
            return UExtracting(f, extractor, condition, { r, c -> newWriter(f, r, c) })
        }

        fun <Z> withJWriter(newWriter: BiConsumer<Any, Z>): UExtracting<T, Z, R> {
            return withWriter { r, c -> newWriter.accept(r, c) }
        }

        fun <Z> withJFWriter(newWriter: TriConsumer<Field, Any?, Z>): UExtracting<T, Z, R> {
            return withFWriter { f, r, c -> newWriter.accept(f, r, c) }
        }

        /*
         * Conditions
         */

        fun withCondition(predicate: (R?) -> Boolean): UExtracting<T, C, R> {
            return UExtracting(f, extractor, condition.also { predicate }, writer)
        }

        fun withJCondition(predicate: Predicate<R?>): UExtracting<T, C, R> {
            return withCondition { predicate.test(it) }
        }

        /*
         * Value decorators
         */

        fun withDecorator(fx: (R) -> R): UExtracting<T, C, R> {
            return UExtracting(f, { s: T ->
                val v1 = this.extractor(s)
                if (v1 != null) fx(v1) else null
            }, condition, this.writer)
        }

        fun withJDecorator(fx: Function<R, R>): UExtracting<T, C, R> {
            return withDecorator { fx.apply(it) }
        }

        /*
         * Transformers: transforms input from type R to type U
         */

        fun <U> withTransformer(fx: (R) -> U): UExtracting<T, C, U> {
            return UExtracting(f, { s: T ->
                val v1 = this.extractor(s)
                if (v1 != null && condition(v1)) fx(v1) else null
            }, writer = this.writer)
        }

        fun <U> withNullTransformer(fx: (R?) -> U?): UExtracting<T, C, U> {
            return UExtracting(f, { s: T ->
                val v1 = this.extractor(s)
                if (condition(v1)) fx(v1) else null
            }, writer = this.writer)
        }

        fun <U> withJTransformer(fx: Function<R, U>): UExtracting<T, C, U> {
            return withTransformer { fx.apply(it) }
        }

        fun ignoreErrors(): UExtracting<T, C, R> {
            return UExtracting(f, { source: T ->
                try {
                    this.extractor(source)
                } catch (e: Exception) {
                    null
                }
            }, condition, writer)
        }
    }

    companion object Factory {
        /**
         * @return most generic convertor which is a function of source object (T) and working context (U)
         */
        fun <T, U> simpleOf(f: Field, cons: BiConsumer<T, U>): Simple<T, U> {
            return Simple(f, { t, c -> cons.accept(t, c) })
        }

        /**
         * @return generic convertor as a function of source object (T), working context (U) and field (f)
         */
        fun <T, U> fSimpleOf(f: Field, cons: TriConsumer<Field, T, U>): Simple<T, U> {
            return Simple(f, { t, c -> cons.accept(f, t, c) })
        }

        /*
         * Convertors with value extractor and associated writer.
         * Type of value returned by extractor should match to the type of the writer
         */

        /**
         *
         */
        fun <T, U, R> extractingOf(f: Field, fx: Function<T, R?>, cons: BiConsumer<R?, U>): Extracting<T, U, R> {
            return Extracting(f, { t -> fx.apply(t) }, writer = { t, c -> if (t != null) cons.accept(t, c) })
        }

        fun <T, U, R> extractingNullOf(f: Field, fx: Function<T, R?>, cons: BiConsumer<R?, U>): Extracting<T, U, R> {
            return Extracting(f, { t -> fx.apply(t) }, writer = { t, c -> cons.accept(t, c) })
        }

        fun <T, U, R> fExtractingOf(f: Field, fx: BiFunction<Field, T, R?>, cons: (Field, R?, U) -> Unit): Extracting<T, U, R> {
            return Extracting(f, { t -> fx.apply(f, t) }, writer = { t, c -> cons(f, t, c) })
        }

        /*
         * Convertors with value extractor and associated generic writer.
         * Writer accepts Object, it is up to writer to handle all possible input types
         */

        fun <T, C, R> uniExtractingOf(f: Field, fx: Function<T, R>): UExtracting<T, C, R> {
            return UExtracting(f, { t -> fx.apply(t) })
        }

        fun <T, C, R> fUniExtractingOf(f: Field, fx: BiFunction<Field, T, R>): UExtracting<T, C, R> {
            return UExtracting(f, { t -> fx.apply(f, t) })
        }
    }
}

