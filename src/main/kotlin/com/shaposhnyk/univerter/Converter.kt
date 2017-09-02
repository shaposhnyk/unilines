package com.shaposhnyk.univerter

/**
 * A field converter, which consumes a source object (T) and a working context (C)
 */
interface Converter<T, C> : Field {
    fun fields(): List<Converter<*, *>>

    fun consume(source: T, ctx: C): Unit
}