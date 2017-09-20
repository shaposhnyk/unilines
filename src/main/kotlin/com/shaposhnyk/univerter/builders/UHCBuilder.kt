package com.shaposhnyk.univerter.builders

import com.shaposhnyk.univerter.UBiPipeline
import com.shaposhnyk.univerter.UField

/**
 * Hierarchical converter builder.
 * Such converter feeds it's input to sub-converters, with eventual transformation
 * T_IN & C_IN  - input source object and working context types
 * T_OUT & C_OUT  - output source object and working context types, also the type of the sub-converters
 */
data class UHCBuilder<in T_IN, C_IN, T_OUT, C_OUT>(
        val f: UField,
        val sFx: (T_IN?) -> T_OUT?,
        val ctxFx: (C_IN) -> C_OUT,
        val fields: MutableList<UBiPipeline<T_OUT, C_OUT>> = mutableListOf()
) : ComposingBuilder<T_IN, C_IN, T_OUT, C_OUT> {

    // Input type specifiers. Do nothing. Compilation time type-safety

    /**
     * Input type specifier. Does nothing, except type inference
     * @param typeRef - used only for type inference
     */
    fun <TX> ofSourceType(typeRef: Class<TX>): UHCBuilder<TX, C_IN, TX, C_OUT> {
        return UHCBuilder(f, { it }, ctxFx)
    }

    /**
     * Input type specifier. Does nothing, except type inference
     * @param typeSup - used only for type inference. Never called
     */
    fun <TX> ofSourceType(typeSup: () -> TX): UHCBuilder<TX, C_IN, TX, C_OUT> {
        return UHCBuilder(f, { it }, ctxFx)
    }

    /**
     * Input type specifier. Does nothing, except type inference
     * @param obj - used only for type inference
     */
    fun <TX> ofSourceType(obj: TX): UHCBuilder<TX, C_IN, TX, C_OUT> {
        return UHCBuilder(f, { it }, ctxFx)
    }

    /**
     * Working context type specifier. Does nothing, except type inference
     * @param typeRef - used only for type inference
     */
    fun <CX> ofContextType(typeRef: Class<CX>): UHCBuilder<T_IN, CX, T_OUT, CX> {
        return UHCBuilder(f, sFx, { it })
    }

    /**
     * Working context type specifier. Does nothing, except type inference
     * @param typeSup - used only for type inference. Never called
     */
    fun <CX> ofContextType(typeSup: () -> CX): UHCBuilder<T_IN, CX, T_OUT, CX> {
        return UHCBuilder(f, sFx, { it })
    }

    /**
     * Working context type specifier. Does nothing, except type inference
     * @param obj - used only for type inference
     */
    fun <CX> ofContextType(obj: CX): UHCBuilder<T_IN, CX, T_OUT, CX> {
        return UHCBuilder(f, sFx, { it })
    }

    // Initializers: this will throw out existing converters

    fun <TX0, TX1> ofSourceMap(newSFx: (TX0?) -> TX1?): UHCBuilder<TX0, C_IN, TX1, C_OUT> {
        return UHCBuilder(f, newSFx, ctxFx)
    }

    fun <TX0, TX1> ofSourceMapF(newSFx: (UField, TX0?) -> TX1?): UHCBuilder<TX0, C_IN, TX1, C_OUT> {
        return ofSourceMap { newSFx(f, it) }
    }

    fun <CX0, CX1> ofContextMap(newCtxF: (CX0) -> CX1): UHCBuilder<T_IN, CX0, T_OUT, CX1> {
        return UHCBuilder(f, sFx, newCtxF)
    }

    fun <CX0, CX1> ofContextMapF(newCtxF: (UField, CX0) -> CX1): UHCBuilder<T_IN, CX0, T_OUT, CX1> {
        return ofContextMap { newCtxF(f, it) }
    }

    // Mappers: do input or context transformation

    fun <X> mapS(afterSFx: (T_OUT?) -> X?): UHCBuilder<T_IN, C_IN, X, C_OUT> {
        return UHCBuilder<T_IN, C_IN, X, C_OUT>(f, { afterSFx(sFx(it)) }, ctxFx)
    }

    fun <X> mapSF(afterSFx: (UField, T_OUT?) -> X?): UHCBuilder<T_IN, C_IN, X, C_OUT> {
        return mapS { afterSFx(f, it) }
    }

    fun <X> mapC(afterCtxFx: (C_OUT) -> X): UHCBuilder<T_IN, C_IN, T_OUT, X> {
        return UHCBuilder<T_IN, C_IN, T_OUT, X>(f, sFx, { afterCtxFx(ctxFx(it)) })
    }

    fun <X> mapCF(afterCtxFx: (UField, C_OUT) -> X): UHCBuilder<T_IN, C_IN, T_OUT, X> {
        return mapC { afterCtxFx(f, it) }
    }

    /**
     * Transform input to an iterable. Every item will be feed to downstream converter
     */
    fun <X> iterateOn(afterSFx: (T_OUT?) -> Iterable<X>): UCObjects.IHCBuilder<T_IN, C_IN, X, C_OUT> {
        return UCObjects.IHCBuilder<T_IN, C_IN, X, C_OUT>(f, { afterSFx(sFx(it)) }, ctxFx)
    }

    /**
     * Add a sub-converter field to this Builder composed of multiple fields
     * Output of transformation will be propagated to all fields
     */
    override fun field(converter: UBiPipeline<T_OUT, C_OUT>): ComposingBuilder<T_IN, C_IN, T_OUT, C_OUT> {
        fields.add(converter)
        return this
    }

    override fun fields(converters: Collection<UBiPipeline<T_OUT, C_OUT>>): ComposingBuilder<T_IN, C_IN, T_OUT, C_OUT> {
        fields.addAll(converters)
        return this
    }

    /**
     * Pipe to downstream converter. Same as field(converter).build()
     */
    fun pipeTo(downstream: UBiPipeline<T_OUT, C_OUT>): UBiPipeline<T_IN, C_IN> {
        if (fields.size > 0) {
            throw IllegalStateException("pipeTo is mutually exclusive with field()")
        }
        return UCObjects.ChainingBiPipeline(f, UCField.Simple(downstream, { t, c ->
            val t1 = sFx(t)
            val c1 = ctxFx(c)
            downstream.consume(t1, c1)
        }))
    }

    /**
     * Builds new converter from the builder
     */
    override fun build(): UBiPipeline<T_IN, C_IN> {
        return UCObjects.DispatchingBiPipeline(f, fields.toList(), sFx, ctxFx)
    }
}