package com.shaposhnyk.univerter

/**
 * Created by vlad on 25.08.17.
 */
interface Convertor<T, C> : Field {

    fun fields(): List<Convertor<*, *>>

    fun consume(source: T, ctx: C): Unit

    fun complete(cons: (T, C) -> Unit)

    fun complete(cons: (T, C) -> Unit, fields: Collection<Convertor<*, *>>)

    fun hierarchical(vararg convs: Convertor<T, C>): Convertor<T, C>

    fun <R> transformInput(ifx: (T) -> R): Convertor<R, C>

    fun <Z> transformContext(cfx: (C) -> Z): Convertor<T, Z>
}