package com.shaposhnyk.unilines

/**
 * Structured UBiPipeline, which consumes a source object (T) and a working context (C)
 * Processing is done by object it-self as well as by underlying sub-pipelines
 */
interface UBiPipeline<in T, C> : UField {
    /**
     * @return an immutable list of sub-converters
     */
    fun fields(): List<UBiPipeline<*, *>>

    /**
     * Process sourceObj object within working context
     * @param sourceObj - input sourceObj object
     * @param workingCtx - working context
     */
    fun consume(sourceObj: T?, workingCtx: C): Unit

    /**
     * Pipeline as T?,C->C function
     */
    fun asBiFunction(): (T?, C) -> C {
        return { source, workingCtx ->
            consume(source, workingCtx)
            workingCtx
        }
    }

    /**
     * Pipeline as T?->C function
     * @param initialCtxSupplier - initial working context supplier
     */
    fun asFunction(initialCtxSupplier: () -> C): (T?) -> C {
        return {
            val workingCtx = initialCtxSupplier()
            consume(it, workingCtx)
            workingCtx
        }
    }
}