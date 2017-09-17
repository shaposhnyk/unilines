package com.shaposhnyk.univerter.builders

import com.shaposhnyk.univerter.Converter
import com.shaposhnyk.univerter.Field

/**
 * Hierarchical converters factory
 */
class Objects {

    interface ComposingBuilder<T0, C0, T1, C1> {
        /**
         * Adds a field to the builder
         */
        fun field(converter: Converter<T1, C1>): ComposingBuilder<T0, C0, T1, C1>

        /**
         * Adds fields to the builder
         */
        fun fields(converters: Collection<Converter<T1, C1>>): ComposingBuilder<T0, C0, T1, C1>

        /**
         * Terminal operation, constructing a converter from the builder
         */
        fun build(): Converter<T0, C0>
    }

    companion object Builder {
        /**
         * @return hierarchical converter builder, which accepts sub-fields of type Converter<T,C>
         */
        fun <T, C> ofField(f: Field): HCBuilder<T, C, T, C> {
            return HCBuilder(f, { it }, { it })
        }
    }

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
    }

    data class HCBuilder<T0, C0, T1, C1>(
            val f: Field,
            val sFx: (T0) -> T1,
            val ctxFx: (C0) -> C1,
            val fields: MutableList<Converter<T1, C1>> = mutableListOf()
    ) : ComposingBuilder<T0, C0, T1, C1> {

        // Input type specifiers. Do nothing. Compilation time type-safety
        fun <TX> ofSourceType(typeRef: Class<TX>): HCBuilder<TX, C0, TX, C1> {
            return HCBuilder(f, { it }, ctxFx)
        }

        fun <TX> ofSourceType(typeSup: () -> TX): HCBuilder<TX, C0, TX, C1> {
            return HCBuilder(f, { it }, ctxFx)
        }

        fun <TX> ofSourceType(obj: TX): HCBuilder<TX, C0, TX, C1> {
            return HCBuilder(f, { it }, ctxFx)
        }

        fun <CX> ofContextType(typeRef: Class<CX>): HCBuilder<T0, CX, T1, CX> {
            return HCBuilder(f, sFx, { it })
        }

        fun <CX> ofContextType(typeSup: () -> CX): HCBuilder<T0, CX, T1, CX> {
            return HCBuilder(f, sFx, { it })
        }

        fun <CX> ofContextType(obj: CX): HCBuilder<T0, CX, T1, CX> {
            return HCBuilder(f, sFx, { it })
        }

        // Initializers: this will throw out existing converters

        fun <TX0, TX1> initialMapS(newSFx: (TX0) -> TX1): HCBuilder<TX0, C0, TX1, C1> {
            return HCBuilder(f, newSFx, ctxFx)
        }

        fun <TX0, TX1> initialMapSF(newSFx: (Field, TX0) -> TX1): HCBuilder<TX0, C0, TX1, C1> {
            return initialMapS { newSFx(f, it) }
        }

        fun <CX0, CX1> initialMapC(newCtxF: (CX0) -> CX1): HCBuilder<T0, CX0, T1, CX1> {
            return HCBuilder(f, sFx, newCtxF)
        }

        fun <CX0, CX1> initialMapCF(newCtxF: (Field, CX0) -> CX1): HCBuilder<T0, CX0, T1, CX1> {
            return initialMapC { newCtxF(f, it) }
        }

        // Mappers: do input or context transformation

        fun <X> mapS(afterSFx: (T1) -> X): HCBuilder<T0, C0, X, C1> {
            return HCBuilder<T0, C0, X, C1>(f, { afterSFx(sFx(it)) }, ctxFx)
        }

        fun <X> mapSF(newSFx: (Field, T1) -> X): HCBuilder<T0, C0, X, C1> {
            return mapS { newSFx(f, it) }
        }

        fun <X> mapC(afterCtxFx: (C1) -> X): HCBuilder<T0, C0, T1, X> {
            return HCBuilder<T0, C0, T1, X>(f, sFx, { afterCtxFx(ctxFx(it)) })
        }

        fun <X> mapCF(afterCtxFx: (Field, C1) -> X): HCBuilder<T0, C0, T1, X> {
            return mapC { afterCtxFx(f, it) }
        }

        // Iteration transformer
        fun <X> flatMapS(newSFx: (T1) -> Iterable<X>): IHCBuilder<T0, C0, X, C1> {
            return IHCBuilder<T0, C0, X, C1>(f, { newSFx(sFx(it)) }, ctxFx)
        }

        /**
         * Add a sub-converter field to this Builder composed of multiple fields
         * Output of transformation will be propagated to all fields
         */
        override fun field(conv: Converter<T1, C1>): ComposingBuilder<T0, C0, T1, C1> {
            fields.add(conv)
            return this
        }

        override fun fields(convs: Collection<Converter<T1, C1>>): ComposingBuilder<T0, C0, T1, C1> {
            fields.addAll(convs)
            return this
        }

        fun pipeTo(downstream: Converter<T1, C1>): Converter<T0, C0> {
            if (fields.size > 0) {
                throw IllegalStateException("pipeTo is mutually exclusive with field()")
            }
            return Simples.Simple(f, { t, c ->
                val t1 = if (t != null) sFx(t) else null
                val c1 = ctxFx(c)
                downstream.consume(t1, c1)
            })
        }

        /**
         * Builds new converter from the builder
         */
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

