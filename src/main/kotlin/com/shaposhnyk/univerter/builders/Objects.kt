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

    /**
     * A converter which delegates processing to another converter,
     * while exposing it in fields()
     */
    data class ChainingConverter<T, C>(
            val conv: Converter<T, C>
    ) : Field by conv, Converter<T, C> {
        override fun fields(): List<Converter<*, *>> = listOf(conv)

        override fun consume(sourceObj: T?, workingCtx: C) {
            if (sourceObj != null) conv.consume(sourceObj, workingCtx)
        }
    }

    /**
     * A converter which delegates processing to several other converters,
     * while exposing then in fields()
     */
    data class DispatchingConverter<T, C, R, Z>(
            val f: Field,
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

    /**
     * Hierarchical converter builder.
     * Such converter feeds it's input to sub-converters, with eventual transformation
     * TIN & CIN  - input source object and working context types
     * TOUT & COUT  - output source object and working context types, also the type of the sub-converters
     */
    data class HCBuilder<TIN, CIN, TOUT, COUT>(
            val f: Field,
            val sFx: (TIN) -> TOUT,
            val ctxFx: (CIN) -> COUT,
            val fields: MutableList<Converter<TOUT, COUT>> = mutableListOf()
    ) : ComposingBuilder<TIN, CIN, TOUT, COUT> {

        // Input type specifiers. Do nothing. Compilation time type-safety

        /**
         * Input type specifier. Does nothing, except type inference
         * @param typeRef - used only for type inference
         */
        fun <TX> ofSourceType(typeRef: Class<TX>): HCBuilder<TX, CIN, TX, COUT> {
            return HCBuilder(f, { it }, ctxFx)
        }

        /**
         * Input type specifier. Does nothing, except type inference
         * @param typeSup - used only for type inference. Never called
         */
        fun <TX> ofSourceType(typeSup: () -> TX): HCBuilder<TX, CIN, TX, COUT> {
            return HCBuilder(f, { it }, ctxFx)
        }

        /**
         * Input type specifier. Does nothing, except type inference
         * @param obj - used only for type inference
         */
        fun <TX> ofSourceType(obj: TX): HCBuilder<TX, CIN, TX, COUT> {
            return HCBuilder(f, { it }, ctxFx)
        }

        /**
         * Working context type specifier. Does nothing, except type inference
         * @param typeRef - used only for type inference
         */
        fun <CX> ofContextType(typeRef: Class<CX>): HCBuilder<TIN, CX, TOUT, CX> {
            return HCBuilder(f, sFx, { it })
        }

        /**
         * Working context type specifier. Does nothing, except type inference
         * @param typeSup - used only for type inference. Never called
         */
        fun <CX> ofContextType(typeSup: () -> CX): HCBuilder<TIN, CX, TOUT, CX> {
            return HCBuilder(f, sFx, { it })
        }

        /**
         * Working context type specifier. Does nothing, except type inference
         * @param obj - used only for type inference
         */
        fun <CX> ofContextType(obj: CX): HCBuilder<TIN, CX, TOUT, CX> {
            return HCBuilder(f, sFx, { it })
        }

        // Initializers: this will throw out existing converters

        fun <TX0, TX1> ofSourceMap(newSFx: (TX0) -> TX1): HCBuilder<TX0, CIN, TX1, COUT> {
            return HCBuilder(f, newSFx, ctxFx)
        }

        fun <TX0, TX1> ofSourceMapF(newSFx: (Field, TX0) -> TX1): HCBuilder<TX0, CIN, TX1, COUT> {
            return ofSourceMap { newSFx(f, it) }
        }

        fun <CX0, CX1> ofContextMap(newCtxF: (CX0) -> CX1): HCBuilder<TIN, CX0, TOUT, CX1> {
            return HCBuilder(f, sFx, newCtxF)
        }

        fun <CX0, CX1> ofContextMapF(newCtxF: (Field, CX0) -> CX1): HCBuilder<TIN, CX0, TOUT, CX1> {
            return ofContextMap { newCtxF(f, it) }
        }

        // Mappers: do input or context transformation

        fun <X> mapS(afterSFx: (TOUT) -> X): HCBuilder<TIN, CIN, X, COUT> {
            return HCBuilder<TIN, CIN, X, COUT>(f, { afterSFx(sFx(it)) }, ctxFx)
        }

        fun <X> mapSF(newSFx: (Field, TOUT) -> X): HCBuilder<TIN, CIN, X, COUT> {
            return mapS { newSFx(f, it) }
        }

        fun <X> mapC(afterCtxFx: (COUT) -> X): HCBuilder<TIN, CIN, TOUT, X> {
            return HCBuilder<TIN, CIN, TOUT, X>(f, sFx, { afterCtxFx(ctxFx(it)) })
        }

        fun <X> mapCF(afterCtxFx: (Field, COUT) -> X): HCBuilder<TIN, CIN, TOUT, X> {
            return mapC { afterCtxFx(f, it) }
        }

        /**
         * Transform input to an iterable. Every item will be feed to downstream converter
         */
        fun <X> iterateOn(afterSFx: (TOUT) -> Iterable<X>): IHCBuilder<TIN, CIN, X, COUT> {
            return IHCBuilder<TIN, CIN, X, COUT>(f, { afterSFx(sFx(it)) }, ctxFx)
        }

        /**
         * Add a sub-converter field to this Builder composed of multiple fields
         * Output of transformation will be propagated to all fields
         */
        override fun field(conv: Converter<TOUT, COUT>): ComposingBuilder<TIN, CIN, TOUT, COUT> {
            fields.add(conv)
            return this
        }

        override fun fields(convs: Collection<Converter<TOUT, COUT>>): ComposingBuilder<TIN, CIN, TOUT, COUT> {
            fields.addAll(convs)
            return this
        }

        /**
         * Pipe to downstream converter. Same as field(converter).build()
         */
        fun pipeTo(downstream: Converter<TOUT, COUT>): Converter<TIN, CIN> {
            if (fields.size > 0) {
                throw IllegalStateException("pipeTo is mutually exclusive with field()")
            }
            return ChainingConverter(Simples.Simple(f, { t, c ->
                val t1 = if (t != null) sFx(t) else null
                val c1 = ctxFx(c)
                downstream.consume(t1, c1)
            }))
        }

        /**
         * Builds new converter from the builder
         */
        override fun build(): Converter<TIN, CIN> {
            return DispatchingConverter(f, fields.toList(), sFx, ctxFx)
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
            return ChainingConverter(simpleConv)
        }
    }
}

