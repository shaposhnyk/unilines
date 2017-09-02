package com.shaposhnyk.univerter

/**
 * Created by vlad on 25.08.17.
 */
interface Convertor<T, C> : Field {
    fun fields(): List<Convertor<*, *>>

    fun consume(source: T, ctx: C): Unit
}