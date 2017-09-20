package com.shaposhnyk.univerter.builders

import com.shaposhnyk.univerter.UBiPipeline
import com.shaposhnyk.univerter.UField

/**
 * Converting objects - converters composed from other converters.
 * They expose their underlying structure using fields() method on UBiPipeline
 */
class UCObjects {

    companion object Builder {
        /**
         * @return hierarchical converter builder, which accepts sub-fields of type UBiPipeline<T,C>
         */
        fun <T, C> of(f: UField): UHCBuilder<T, C, T, C> {
            return UHCBuilder(f, { it }, { it })
        }
    }

    /**
     * A converter which delegates processing to another converter,
     * while exposing it in fields()
     */
    data class ChainingBiPipeline<in T, C>(
            val f: UField,
            val converter: UBiPipeline<T, C>
    ) : UField by f, UBiPipeline<T, C> {
        override fun fields(): List<UBiPipeline<*, *>> = listOf(converter)

        override fun consume(sourceObj: T?, workingCtx: C) {
            converter.consume(sourceObj, workingCtx)
        }
    }

    data class FlatChainingBiPipeline<in TIN, CIN, TOUT, COUT>(
            val f: UField,
            val sFx: (TIN?) -> Iterable<TOUT>,
            val ctxFx: (CIN) -> COUT,
            val converter: UBiPipeline<TOUT, COUT>
    ) : UField by f, UBiPipeline<TIN, CIN> {
        override fun fields(): List<UBiPipeline<*, *>> = listOf(converter)

        override fun consume(sourceObj: TIN?, workingCtx: CIN) {
            val s1 = sFx(sourceObj)
            val c1 = ctxFx(workingCtx)
            s1.forEach { converter.consume(it, c1) }
        }
    }

    /**
     * A converter which delegates processing to several other converters,
     * while exposing then in fields()
     */
    data class DispatchingBiPipeline<in TIN, CIN, TOUT, COUT>(
            val f: UField,
            val fields: List<UBiPipeline<TOUT, COUT>>, // sub-converters
            val sourceFx: (TIN?) -> TOUT?, // source object transformer
            val ctxFx: (CIN) -> COUT) // working context transfromer
        : UField by f, UBiPipeline<TIN, CIN> {
        override fun fields(): List<UBiPipeline<*, *>> = fields

        override fun consume(sourceObj: TIN?, workingCtx: CIN) {
            try {
                val s1 = sourceFx(sourceObj)
                val ctx1 = ctxFx(workingCtx)
                fields.forEach { it.consume(s1, ctx1) }
            } catch (e: RuntimeException) {
                val trace = StackTraceElement("com.shaposhnyk.univerter", "generated", f.toString(), 1)
                e.stackTrace = arrayOf(trace).plus(e.stackTrace)
                throw e
            }
        }
    }

    data class IHCBuilder<in T_IN, C_IN, T_OUT, C_OUT>(
            val f: UField,
            val sFx: (T_IN?) -> Iterable<T_OUT>,
            val ctxFx: (C_IN) -> C_OUT
    ) {
        fun pipeTo(dispatcher: UBiPipeline<T_OUT, C_OUT>): UBiPipeline<T_IN, C_IN> {
            return FlatChainingBiPipeline(f, sFx, ctxFx, dispatcher)
        }
    }
}

