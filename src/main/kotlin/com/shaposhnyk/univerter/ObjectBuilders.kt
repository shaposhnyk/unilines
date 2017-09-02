package com.shaposhnyk.univerter

import java.util.function.Function

/**
 * Created by vlad on 25.08.17.
 */
class ObjectBuilders {

    data class Delegating<T, C, R>(val conv: Convertor<R, C>,
                                   val consumer: (T, C) -> Unit = { _, _ -> Unit })
        : Field by conv, Convertor<T, C> {
        override fun fields(): List<Convertor<*, *>> = listOf(conv)

        override fun consume(source: T, ctx: C) {
            consumer(source, ctx)
        }
    }

    data class Composing<T, C, R, Z>(val f: Field, val fields: List<Convertor<R, Z>>,
                                     val sourceFx: (T) -> R, val ctxFx: (C) -> Z)
        : Field by f, Convertor<T, C> {
        override fun fields(): List<Convertor<*, *>> = fields

        override fun consume(source: T, ctx: C) {
            fields.forEach { it.consume(sourceFx(source), ctxFx(ctx)) }
        }
    }

    data class HBuilder<T, C>(val f: Builders.Simple<T, C>, val fields: MutableList<Convertor<*, *>>) {

        fun field(c: Convertor<*, *>) {
            fields.add(c)
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

        fun <T, C> iterOnInput(conv: Convertor<T, C>): Delegating<Iterable<T>, C, T> {
            return Delegating(conv, { source: Iterable<T>, ctx: C ->
                source.forEach { conv.consume(it, ctx) }
            })
        }

        fun <T, C, R> iterOnInput(conv: Convertor<R, C>, fx: Function<T, Iterable<R>>): Delegating<T, C, R> {
            return Delegating(conv, { source: T, ctx: C ->
                fx.apply(source).forEach { conv.consume(it, ctx) }
            })
        }

        fun <T, C> compose(f: Field, fields: List<Convertor<T, C>>): Composing<T, C, T, C> {
            return Composing(f, fields, { it }, { it })
        }

        fun <T, C, R> composeOnInput(f: Field, fields: List<Convertor<R, C>>, sFx: Function<T, R>): Composing<T, C, R, C> {
            return Composing(f, fields, { sFx.apply(it) }, { it })
        }

        fun <T, C, Z> composeOnContext(f: Field, fields: List<Convertor<T, Z>>, cFx: Function<C, Z>): Composing<T, C, T, Z> {
            return Composing(f, fields, { it }, { cFx.apply(it) })
        }
    }
}

