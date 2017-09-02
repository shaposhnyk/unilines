package com.shaposhnyk.univerter

import java.util.function.BiFunction
import java.util.function.Function

/**
 * Hierarchical convertors factory
 */
class ObjectBuilders {

    data class Delegating<T, C, R>(val conv: Converter<R, C>,
                                   val consumer: (T, C) -> Unit = { _, _ -> Unit })
        : Field by conv, Converter<T, C> {
        override fun fields(): List<Converter<*, *>> = listOf(conv)

        override fun consume(source: T, ctx: C) {
            consumer(source, ctx)
        }
    }

    data class Composing<T, C, R, Z>(val f: Field, val fields: List<Converter<R, Z>>,
                                     val sourceFx: (T) -> R, val ctxFx: (C) -> Z)
        : Field by f, Converter<T, C> {
        override fun fields(): List<Converter<*, *>> = fields

        override fun consume(source: T, ctx: C) {
            val s1 = sourceFx(source)
            val ctx1 = ctxFx(ctx)
            fields.forEach { it.consume(s1, ctx1) }
        }

        fun decorateContext(afterCtx: (Z) -> Z): Composing<T, C, R, Z> {
            return Composing(f, fields, sourceFx, { c: C -> afterCtx(ctxFx(c)) })
        }

        fun decorateFContext(afterCtx: (Field, Z) -> Z): Composing<T, C, R, Z> {
            return Composing(f, fields, sourceFx, { c: C -> afterCtx(f, ctxFx(c)) })
        }

        fun decorateJFContext(afterCtx: BiFunction<Field, Z, Z>): Composing<T, C, R, Z> {
            return decorateContext { afterCtx.apply(f, it) }
        }
    }

    data class HBuilder<T, C>(val f: Builders.Simple<T, C>, val fields: MutableList<Converter<T, C>>) {

        fun field(c: Converter<T, C>): HBuilder<T, C> {
            fields.add(c)
            return this
        }

        fun composer(): Composing<T, C, T, C> {
            return compose(f.f, fields.toList())
        }

        fun <E> composer(fx: Function<E, T>): Composing<E, C, T, C> {
            return composeOnInput(f.f, fields.toList(), fx)
        }
    }

    companion object Factory {

        fun <T, C> of(f: Field, source: T?, ctx: C?): HBuilder<T, C> {
            val simple: Builders.Simple<T, C> = Builders.Simple(f, { t: T, c: C -> Unit })
            return HBuilder(simple, mutableListOf())
        }

        /*
         * Hierarchical converters
         */

        fun <T, C> iterOnInput(conv: Converter<T, C>): Delegating<Iterable<T>, C, T> {
            return Delegating(conv, { source: Iterable<T>, ctx: C ->
                source.forEach { conv.consume(it, ctx) }
            })
        }

        fun <T, C, R> iterOnInput(conv: Converter<R, C>, fx: Function<T, Iterable<R>>): Delegating<T, C, R> {
            return Delegating(conv, { source: T, ctx: C ->
                fx.apply(source).forEach { conv.consume(it, ctx) }
            })
        }

        fun <T, C> compose(f: Field, fields: List<Converter<T, C>>): Composing<T, C, T, C> {
            return Composing(f, fields, { it }, { it })
        }

        fun <T, C, R> composeOnInput(f: Field, fields: List<Converter<R, C>>, sFx: Function<T, R>): Composing<T, C, R, C> {
            return Composing(f, fields, { sFx.apply(it) }, { it })
        }

        fun <T, C, Z> composeOnContext(f: Field, fields: List<Converter<T, Z>>, cFx: Function<C, Z>): Composing<T, C, T, Z> {
            return Composing(f, fields, { it }, { cFx.apply(it) })
        }
    }
}

