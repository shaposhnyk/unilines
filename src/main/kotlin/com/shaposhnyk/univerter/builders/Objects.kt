package com.shaposhnyk.univerter.builders

import com.shaposhnyk.univerter.Converter
import com.shaposhnyk.univerter.Field

/**
 * Hierarchical converters factory
 */
class Objects {

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
            val f: Field,
            val conv: Converter<T, C>
    ) : Field by f, Converter<T, C> {
        override fun fields(): List<Converter<*, *>> = listOf(conv)

        override fun consume(sourceObj: T?, workingCtx: C) {
            conv.consume(sourceObj, workingCtx)
        }
    }

    data class FlatChainingConverter<TIN, CIN, TOUT, COUT>(
            val f: Field,
            val sFx: (TIN?) -> Iterable<TOUT>,
            val ctxFx: (CIN) -> COUT,
            val conv: Converter<TOUT, COUT>
    ) : Field by f, Converter<TIN, CIN> {
        override fun fields(): List<Converter<*, *>> = listOf(conv)

        override fun consume(sourceObj: TIN?, workingCtx: CIN) {
            val s1 = sFx(sourceObj)
            val c1 = ctxFx(workingCtx)
            s1.forEach { conv.consume(it, c1) }
        }
    }

    /**
     * A converter which delegates processing to several other converters,
     * while exposing then in fields()
     */
    data class DispatchingConverter<TIN, CIN, TOUT, COUT>(
            val f: Field,
            val fields: List<Converter<TOUT, COUT>>, // sub-converters
            val sourceFx: (TIN?) -> TOUT?, // source object transformer
            val ctxFx: (CIN) -> COUT) // working context transfromer
        : Field by f, Converter<TIN, CIN> {
        override fun fields(): List<Converter<*, *>> = fields

        override fun consume(sourceObj: TIN?, workingCtx: CIN) {
            val s1 = sourceFx(sourceObj)
            val ctx1 = ctxFx(workingCtx)
            fields.forEach { it.consume(s1, ctx1) }
        }
    }

    /**
     * Hierarchical converter builder.
     * Such converter feeds it's input to sub-converters, with eventual transformation
     * T_IN & C_IN  - input source object and working context types
     * T_OUT & C_OUT  - output source object and working context types, also the type of the sub-converters
     */
    data class HCBuilder<T_IN, C_IN, T_OUT, C_OUT>(
            val f: Field,
            val sFx: (T_IN?) -> T_OUT?,
            val ctxFx: (C_IN) -> C_OUT,
            val fields: MutableList<Converter<T_OUT, C_OUT>> = mutableListOf()
    ) : ComposingBuilder<T_IN, C_IN, T_OUT, C_OUT> {

        // Input type specifiers. Do nothing. Compilation time type-safety

        /**
         * Input type specifier. Does nothing, except type inference
         * @param typeRef - used only for type inference
         */
        fun <TX> ofSourceType(typeRef: Class<TX>): HCBuilder<TX, C_IN, TX, C_OUT> {
            return HCBuilder(f, { it }, ctxFx)
        }

        /**
         * Input type specifier. Does nothing, except type inference
         * @param typeSup - used only for type inference. Never called
         */
        fun <TX> ofSourceType(typeSup: () -> TX): HCBuilder<TX, C_IN, TX, C_OUT> {
            return HCBuilder(f, { it }, ctxFx)
        }

        /**
         * Input type specifier. Does nothing, except type inference
         * @param obj - used only for type inference
         */
        fun <TX> ofSourceType(obj: TX): HCBuilder<TX, C_IN, TX, C_OUT> {
            return HCBuilder(f, { it }, ctxFx)
        }

        /**
         * Working context type specifier. Does nothing, except type inference
         * @param typeRef - used only for type inference
         */
        fun <CX> ofContextType(typeRef: Class<CX>): HCBuilder<T_IN, CX, T_OUT, CX> {
            return HCBuilder(f, sFx, { it })
        }

        /**
         * Working context type specifier. Does nothing, except type inference
         * @param typeSup - used only for type inference. Never called
         */
        fun <CX> ofContextType(typeSup: () -> CX): HCBuilder<T_IN, CX, T_OUT, CX> {
            return HCBuilder(f, sFx, { it })
        }

        /**
         * Working context type specifier. Does nothing, except type inference
         * @param obj - used only for type inference
         */
        fun <CX> ofContextType(obj: CX): HCBuilder<T_IN, CX, T_OUT, CX> {
            return HCBuilder(f, sFx, { it })
        }

        // Initializers: this will throw out existing converters

        fun <TX0, TX1> ofSourceMap(newSFx: (TX0?) -> TX1?): HCBuilder<TX0, C_IN, TX1, C_OUT> {
            return HCBuilder(f, newSFx, ctxFx)
        }

        fun <TX0, TX1> ofSourceMapF(newSFx: (Field, TX0?) -> TX1?): HCBuilder<TX0, C_IN, TX1, C_OUT> {
            return ofSourceMap { newSFx(f, it) }
        }

        fun <CX0, CX1> ofContextMap(newCtxF: (CX0) -> CX1): HCBuilder<T_IN, CX0, T_OUT, CX1> {
            return HCBuilder(f, sFx, newCtxF)
        }

        fun <CX0, CX1> ofContextMapF(newCtxF: (Field, CX0) -> CX1): HCBuilder<T_IN, CX0, T_OUT, CX1> {
            return ofContextMap { newCtxF(f, it) }
        }

        // Mappers: do input or context transformation

        fun <X> mapS(afterSFx: (T_OUT?) -> X?): HCBuilder<T_IN, C_IN, X, C_OUT> {
            return HCBuilder<T_IN, C_IN, X, C_OUT>(f, { afterSFx(sFx(it)) }, ctxFx)
        }

        fun <X> mapSF(newSFx: (Field, T_OUT?) -> X?): HCBuilder<T_IN, C_IN, X, C_OUT> {
            return mapS { newSFx(f, it) }
        }

        fun <X> mapC(afterCtxFx: (C_OUT) -> X): HCBuilder<T_IN, C_IN, T_OUT, X> {
            return HCBuilder<T_IN, C_IN, T_OUT, X>(f, sFx, { afterCtxFx(ctxFx(it)) })
        }

        fun <X> mapCF(afterCtxFx: (Field, C_OUT) -> X): HCBuilder<T_IN, C_IN, T_OUT, X> {
            return mapC { afterCtxFx(f, it) }
        }

        /**
         * Transform input to an iterable. Every item will be feed to downstream converter
         */
        fun <X> iterateOn(afterSFx: (T_OUT?) -> Iterable<X>): IHCBuilder<T_IN, C_IN, X, C_OUT> {
            return IHCBuilder<T_IN, C_IN, X, C_OUT>(f, { afterSFx(sFx(it)) }, ctxFx)
        }

        /**
         * Add a sub-converter field to this Builder composed of multiple fields
         * Output of transformation will be propagated to all fields
         */
        override fun field(converter: Converter<T_OUT, C_OUT>): ComposingBuilder<T_IN, C_IN, T_OUT, C_OUT> {
            fields.add(converter)
            return this
        }

        override fun fields(converters: Collection<Converter<T_OUT, C_OUT>>): ComposingBuilder<T_IN, C_IN, T_OUT, C_OUT> {
            fields.addAll(converters)
            return this
        }

        /**
         * Pipe to downstream converter. Same as field(converter).build()
         */
        fun pipeTo(downstream: Converter<T_OUT, C_OUT>): Converter<T_IN, C_IN> {
            if (fields.size > 0) {
                throw IllegalStateException("pipeTo is mutually exclusive with field()")
            }
            return ChainingConverter(f, Simples.Simple(downstream, { t, c ->
                val t1 = sFx(t)
                val c1 = ctxFx(c)
                downstream.consume(t1, c1)
            }))
        }

        /**
         * Builds new converter from the builder
         */
        override fun build(): Converter<T_IN, C_IN> {
            return DispatchingConverter(f, fields.toList(), sFx, ctxFx)
        }
    }

    data class IHCBuilder<T_IN, C_IN, T_OUT, C_OUT>(
            val f: Field,
            val sFx: (T_IN?) -> Iterable<T_OUT>,
            val ctxFx: (C_IN) -> C_OUT
    ) {
        fun pipeTo(dispatcher: Converter<T_OUT, C_OUT>): Converter<T_IN, C_IN> {
            return FlatChainingConverter(f, sFx, ctxFx, dispatcher)
        }
    }
}

