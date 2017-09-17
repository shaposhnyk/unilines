package com.shaposhnyk.univerter

/**
 * A field converter, which consumes a source object (T) and a working context (C)
 */
interface Converter<in T, in C> : Field {
    /**
     * @return an immutable list of sub-converters
     */
    fun fields(): List<Converter<*, *>>

    /**
     * Process sourceObj object within working context
     * @param sourceObj - input sourceObj object
     * @param workingCtx - working context
     */
    fun consume(sourceObj: T?, workingCtx: C): Unit
}