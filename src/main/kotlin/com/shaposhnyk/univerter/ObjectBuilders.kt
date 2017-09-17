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

        override fun consume(source: T?, ctx: C) {
            if (source != null) consumer(source, ctx)
        }
    }

    data class Composing<T, C, R, Z>(val f: Field,
                                     val fields: List<Converter<R, Z>>,
                                     val sourceFx: (T) -> R,
                                     val ctxFx: (C) -> Z)
        : Field by f, Converter<T, C> {
        override fun fields(): List<Converter<*, *>> = fields

        override fun consume(source: T?, ctx: C) {
            if (source != null) {
                val s1 = sourceFx(source)
                val ctx1 = ctxFx(ctx)
                fields.forEach { it.consume(s1, ctx1) }
            }
        }

        fun decorateContext(afterCtx: (Z) -> Z): Composing<T, C, R, Z> {
            return Composing(f, fields, sourceFx, { c: C -> afterCtx(ctxFx(c)) })
        }

        fun decorateContextF(afterCtx: (Field, Z) -> Z): Composing<T, C, R, Z> {
            return Composing(f, fields, sourceFx, { c: C -> afterCtx(f, ctxFx(c)) })
        }

        fun decorateJFC(afterCtx: BiFunction<Field, Z, Z>): Composing<T, C, R, Z> {
            return decorateContext { afterCtx.apply(f, it) }
        }
    }

    data class HBuilder<T, C>(val f: Builders.Simple<T, C>, val fields: MutableList<Converter<T, C>>) {

        fun field(c: Converter<T, C>): HBuilder<T, C> {
            fields.add(c)
            return this
        }

        fun build(): Composing<T, C, T, C> {
            return compose(f.f, fields.toList())
        }

        fun <E> mapJS(fx: Function<E, T>): Composing<E, C, T, C> {
            return composeOnInput(f.f, fields.toList(), { fx.apply(it) })
        }

        fun <E> mapJFS(bfx: BiFunction<Field, E, T>): Composing<E, C, T, C> {
            return composeOnInput(f.f, fields.toList(), { it: E -> bfx.apply(f.f, it) })
        }

        fun <E> flatMapJS(fx: Function<E, Iterable<T>>): Converter<E, C> {
            val comps = Composing<T, C, T, C>(f.f, fields.toList(), { it: T -> it }, { it })
            val simple: Builders.Simple<E, C> = Builders.Simple(f)
            return Delegating<E, C, E>(simple, { e: E, c: C ->
                fx.apply(e).forEach { t -> comps.consume(t, c) }
            })
        }
    }

    companion object Factory {
        /**
         * @return hierarchical converter builder, which accepts sub-fields of type Converter<T,C>
         */
        fun <T, C> of(f: Field): HBuilder<T, C> {
            val simple: Builders.Simple<T, C> = Builders.Simple(f)
            return HBuilder(simple, mutableListOf())
        }

        /**
         * Quite ugly, but still useful in some cases
         * @return hierarchical converter builder, which accepts sub-fields of type Converter<T,C>
         */
        fun <T, C> of(f: Field, source: T?, ctx: C?): HBuilder<T, C> {
            val simple: Builders.Simple<T, C> = Builders.Simple(f)
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

        fun <T, C, R> composeOnInput(f: Field, fields: List<Converter<R, C>>, sFx: (T) -> R): Composing<T, C, R, C> {
            return Composing(f, fields, sFx, { it })
        }

        fun <T, C, Z> composeOnContext(f: Field, fields: List<Converter<T, Z>>, cFx: (C) -> Z): Composing<T, C, T, Z> {
            return Composing(f, fields, { it }, cFx)
        }

        fun <T, C> ofRoot(f: Field): HCBuilder<T, C, T, C> {
            return HCBuilder(f, { it }, { it })
        }

        fun <T, C> ofRoot(f: Field, s: T, c: C): HCBuilder<T, C, T, C> {
            return HCBuilder(f, { it }, { it })
        }
    }

    data class HCBuilder<T0, C0, T1, C1>(
            val f: Field,
            val sFx: (T0) -> T1,
            val ctxFx: (C0) -> C1,
            val fields: MutableList<Converter<T1, C1>> = mutableListOf()
    ) {
        fun <X> mapS(newCtxF: (T1) -> X): HCBuilder<T0, C0, X, C1> {
            return HCBuilder<T0, C0, X, C1>(f, { newCtxF(sFx(it)) }, ctxFx)
        }

        fun <X> mapC(newCtxF: (C1) -> X): HCBuilder<T0, C0, T1, X> {
            return HCBuilder<T0, C0, T1, X>(f, sFx, { newCtxF(ctxFx(it)) })
        }

        fun <X> mapCF(newCtxF: (Field, C1) -> X): HCBuilder<T0, C0, T1, X> {
            return mapC { newCtxF(f, it) }
        }

        fun <X> mapSF(newSFx: (Field, T1) -> X): HCBuilder<T0, C0, X, C1> {
            return mapS { newSFx(f, it) }
        }

        fun <X> mapJC(newCtxF: Function<C1, X>): HCBuilder<T0, C0, T1, X> {
            return mapC { newCtxF.apply(it) }
        }

        fun <X> flatMapS(newSFx: (T1) -> Iterable<X>): IHCBuilder<T0, C0, X, C1> {
            return IHCBuilder<T0, C0, X, C1>(f, { newSFx(sFx(it)) }, ctxFx)
        }

        fun field(c: Converter<T1, C1>): HCBuilder<T0, C0, T1, C1> {
            fields.add(c)
            return this
        }

        fun pipeTo(downstream: Converter<T1, C1>): Converter<T0, C0> {
            return Builders.Simple(f, { t, c ->
                val t1 = if (t != null) sFx(t) else null
                val c1 = ctxFx(c)
                downstream.consume(t1, c1)
            })
        }

        fun build(): Converter<T0, C0> {
            return Composing(f, fields.toList(), sFx, ctxFx)
        }
    }

    data class IHCBuilder<T0, C0, T1, C1>(
            val f: Field,
            val sFx: (T0) -> Iterable<T1>,
            val ctxFx: (C0) -> C1
    ) {
        fun pipeTo(downstream: Converter<T1, C1>): Converter<T0, C0> {
            return Builders.Simple(f, { t, c ->
                val t1 = if (t != null) sFx(t) else null
                if (t1 != null) {
                    val c1 = ctxFx(c)
                    t1.forEach { downstream.consume(it, c1) }
                }
            })
        }
    }
}

