package com.shaposhnyk.unilines.builders

import com.shaposhnyk.unilines.UBiPipeline
import com.shaposhnyk.unilines.UField
import com.shaposhnyk.unilines.UTriConsumer

/**
 * Converting objects - converters composed from other converters.
 * They expose their underlying structure using fields() method on UBiPipeline
 */
class UCObjects {

    companion object Builder {
        /**
         * @return hierarchical downstream builder, which accepts sub-fields of type UBiPipeline<T,C>
         */
        fun <T, C> of(f: UField): UHCBuilder<T, C, T, C> {
            return UHCBuilder(f, { it }, { it })
        }
    }

    /**
     * A downstream which delegates processing to another downstream,
     * while exposing it in fields()
     */
    data class ChainingBiPipeline<in T, C>(
            private val f: UField,
            private val downstreams: List<UBiPipeline<*, *>>,
            private val consumer: (T?, C) -> Unit
    ) : UField by f, UBiPipeline<T, C> {
        override fun fields(): List<UBiPipeline<*, *>> = downstreams

        override fun consume(sourceObj: T?, workingCtx: C) {
            try {
                consumer(sourceObj, workingCtx)
            } catch (e: RuntimeException) {
                UCField.defaultErrorHandler(this, e)
            }
        }

        fun postProcess(postProcessor: (UField, UBiPipeline<*, *>, C) -> Unit): ChainingBiPipeline<T, C> {
            assert(downstreams.size == 1)
            return ChainingBiPipeline(f, downstreams, { _, c -> postProcessor(f, downstreams[0], c) })
        }

        fun postProcess(postProcessor: (C) -> Unit): ChainingBiPipeline<T, C> {
            return ChainingBiPipeline(f, downstreams, { _, c -> postProcessor(c) })
        }

        fun postProcess(postProcessor: (UBiPipeline<*, *>, C) -> Unit): ChainingBiPipeline<T, C> {
            return postProcess({ _, cnv, c -> postProcessor(cnv, c) })
        }

        fun postProcessJ(postProcessor: UTriConsumer<UField, UBiPipeline<*, *>, C>): ChainingBiPipeline<T, C> {
            return postProcess({ f, cnv, c -> postProcessor.accept(f, cnv, c) })
        }
    }

    data class UCHFlatBuilder<in T_IN, C_IN, T_OUT, C_OUT>(
            private val f: UField,
            private val sFx: (T_IN?) -> Iterable<T_OUT>,
            private val ctxFx: (C_IN) -> C_OUT,
            private val downstreams: MutableList<UBiPipeline<T_OUT, C_OUT>> = mutableListOf()
    ) : ComposingBuilder<T_IN, C_IN, T_OUT, C_OUT> {

        override fun fields(): List<UBiPipeline<*, *>> = downstreams.toList()

        override fun field(converter: UBiPipeline<T_OUT, C_OUT>): ComposingBuilder<T_IN, C_IN, T_OUT, C_OUT> {
            downstreams.add(converter)
            return this
        }

        override fun build(): UBiPipeline<T_IN, C_IN> {
            return FlatChainingBiPipeline(f, downstreams.toList(), sFx, ctxFx, { t, c ->
                downstreams.forEach { d ->
                    d.consume(t, c)
                }
            })
        }
    }

    /**
     * A downstream which delegates processing to another downstream,
     * while exposing it in fields()
     */
    data class FlatChainingBiPipeline<in T_IN, C_IN, T_OUT, C_OUT>(
            private val f: UField,
            private val downstreams: List<UBiPipeline<*, *>>,
            private val sFx: (T_IN?) -> Iterable<T_OUT>,
            private val ctxFx: (C_IN) -> C_OUT,
            private val consumer: (T_OUT, C_OUT) -> Unit
    ) : UField by f, UBiPipeline<T_IN, C_IN> {

        override fun fields(): List<UBiPipeline<*, *>> = downstreams

        override fun consume(sourceObj: T_IN?, workingCtx: C_IN) {
            var cnt = 0
            try {
                val t1s = sFx(sourceObj)
                val c1 = ctxFx(workingCtx)
                t1s.forEach { it ->
                    cnt += 1
                    consumer(it, c1)
                }
            } catch (e: RuntimeException) {
                UCField.defaultErrorHandler(this, e, cnt)
            }
        }
    }
}

