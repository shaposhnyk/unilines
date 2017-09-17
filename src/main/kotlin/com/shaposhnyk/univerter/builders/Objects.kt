package com.shaposhnyk.univerter.builders

import com.shaposhnyk.univerter.Converter
import com.shaposhnyk.univerter.Field
import java.util.function.BiFunction
import java.util.function.Function

/**
 * Hierarchical converters factory
 */
class Objects {

    data class Delegating<T, C, R>(val conv: Converter<R, C>,
                                   val consumer: (T, C) -> Unit = { _, _ -> Unit })
        : Field by conv, Converter<T, C> {
        override fun fields(): List<Converter<*, *>> = listOf(conv)

        override fun consume(sourceObj: T?, workingCtx: C) {
            if (sourceObj != null) consumer(sourceObj, workingCtx)
        }
    }

    data class Composing<T, C, R, Z>(val f: Field,
                                     val fields: List<Converter<R, Z>>,
                                     val sourceFx: (T) -> R,
                                     val ctxFx: (C) -> Z)
        : Field by f, Converter<T, C> {
        override fun fields(): List<Converter<*, *>> = fields

        override fun consume(sourceObj: T?, workingCtx: C) {
            if (sourceObj != null) {
                val s1 = sourceFx(sourceObj)
                val ctx1 = ctxFx(workingCtx)
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

    data class HBuilder<T, C>(val f: Simples.Simple<T, C>, val fields: MutableList<Converter<T, C>>) {

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
            val simple: Simples.Simple<E, C> = Simples.Simple(f)
            return Delegating<E, C, E>(simple, { e: E, c: C ->
                fx.apply(e).forEach { t -> comps.consume(t, c) }
            })
        }
    }

    companion object Builder {
        /**
         * @return hierarchical converter builder, which accepts sub-fields of type Converter<T,C>
         */
        fun <T, C> of(f: Field): HBuilder<T, C> {
            val simple: Simples.Simple<T, C> = Simples.Simple(f)
            return HBuilder(simple, mutableListOf())
        }

        /**
         * Quite ugly, but still useful in some cases
         * @return hierarchical converter builder, which accepts sub-fields of type Converter<T,C>
         */
        fun <T, C> of(f: Field, source: T?, ctx: C?): HBuilder<T, C> {
            val simple: Simples.Simple<T, C> = Simples.Simple(f)
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

        fun <T, C> ofField(f: Field): HCBuilder<T, C, T, C> {
            return HCBuilder(f, { it }, { it })
        }

        fun <C0, C1> ofTransformingC(f: Field, cFx: (C0) -> C1): HCBuilder<Any, C0, Any, C1> {
            return HCBuilder(f, { it }, cFx)
        }

        fun <T, C> ofRoot(f: Field, s: T, c: C): HCBuilder<T, C, T, C> {
            return HCBuilder(f, { it }, { it })
        }
    }

    interface ComposingBuilder<T0, C0, T1, C1> {

        /**
         * Adds a field to a convertor builder
         */
        fun field(c: Converter<T1, C1>): ComposingBuilder<T0, C0, T1, C1>

        /**
         * Terminal operation, constructing a converter from the builder
         */
        fun build(): Converter<T0, C0>
    }

    data class HCBuilder<T0, C0, T1, C1>(
            val f: Field,
            val sFx: (T0) -> T1,
            val ctxFx: (C0) -> C1,
            val fields: MutableList<Converter<T1, C1>> = mutableListOf()
    ) : ComposingBuilder<T0, C0, T1, C1> {

        fun <TX> ofSourceType(typeRef: Class<TX>): HCBuilder<TX, C0, TX, C1> {
            return HCBuilder(f, { it }, ctxFx)
        }

        fun <TX> ofSourceType(typeSup: () -> TX): HCBuilder<TX, C0, TX, C1> {
            return HCBuilder(f, { it }, ctxFx)
        }

        fun <CX> ofContextType(typeRef: Class<CX>): HCBuilder<T0, CX, T1, CX> {
            return HCBuilder(f, sFx, { it })
        }

        fun <CX> ofContextType(typeSup: () -> CX): HCBuilder<T0, CX, T1, CX> {
            return HCBuilder(f, sFx, { it })
        }

        fun <X> mapS(newCtxF: (T1) -> X): HCBuilder<T0, C0, X, C1> {
            return HCBuilder<T0, C0, X, C1>(f, { newCtxF(sFx(it)) }, ctxFx)
        }

        fun <CX0, CX1> initialMapC(newCtxF: (CX0) -> CX1): HCBuilder<T0, CX0, T1, CX1> {
            return HCBuilder(f, sFx, newCtxF)
        }

        fun <CX0, CX1> initialMapCF(newCtxF: (Field, CX0) -> CX1): HCBuilder<T0, CX0, T1, CX1> {
            return initialMapC { newCtxF(f, it) }
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

        fun <X> flatMapS(newSFx: (T1) -> Iterable<X>): IHCBuilder<T0, C0, X, C1> {
            return IHCBuilder<T0, C0, X, C1>(f, { newSFx(sFx(it)) }, ctxFx)
        }

        override fun field(c: Converter<T1, C1>): ComposingBuilder<T0, C0, T1, C1> {
            fields.add(c)
            return this
        }

        fun pipeTo(downstream: Converter<T1, C1>): Converter<T0, C0> {
            return Simples.Simple(f, { t, c ->
                val t1 = if (t != null) sFx(t) else null
                val c1 = ctxFx(c)
                downstream.consume(t1, c1)
            })
        }

        override fun build(): Converter<T0, C0> {
            return Composing(f, fields.toList(), sFx, ctxFx)
        }
    }

    data class IHCBuilder<T0, C0, T1, C1>(
            val f: Field,
            val sFx: (T0) -> Iterable<T1>,
            val ctxFx: (C0) -> C1
    ) {
        fun pipeTo(downstream: Converter<T1, C1>): Converter<T0, C0> {
            val simpleConv = Simples.Simple<T0, C0>(f, { t, c ->
                val t1 = if (t != null) sFx(t) else null
                if (t1 != null) {
                    val c1 = ctxFx(c)
                    t1.forEach { downstream.consume(it, c1) }
                }
            })
            return Delegating(simpleConv, { t, c -> simpleConv.consume(t, c) })
        }
    }
}

