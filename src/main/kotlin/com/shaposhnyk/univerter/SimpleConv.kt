package com.shaposhnyk.univerter

/**
 * Created by vlad on 25.08.17.
 */
class SimpleConv<T, C>(val holder: Field,
                       var fx: (T, C) -> Unit = { T, C -> Unit },
                       var fields: MutableList<Convertor<*, *>> = mutableListOf()
) : Field by holder, Convertor<T, C> {
    override fun fields(): List<Convertor<*, *>> {
        return fields.toList()
    }

    override fun complete(cons: (T, C) -> Unit, finalFields: Collection<Convertor<*, *>>) {
        complete(cons)
        finalFields.forEach { f -> fields.add(f) }
    }

    override fun complete(cons: (T, C) -> Unit) {
        this.fx = cons
    }

    override fun hierarchical(vararg convs: Convertor<T, C>): Convertor<T, C> {
        complete { s: T, c: C ->
            convs.forEach { cnv -> cnv.consume(s, c) }
        }
        return this
    }

    override fun <R> transformInput(ifx: (T) -> R): Convertor<R, C> {
        val sub: Convertor<R, C> = SimpleConv<R, C>(holder)
        complete({ s: T, c: C -> sub.consume(ifx(s), c) }, listOf(sub))
        return sub
    }

    override fun <Z> transformContext(cfx: (C) -> Z): Convertor<T, Z> {
        val sub = SimpleConv<T, Z>(holder)
        complete({ s: T, c: C -> sub.consume(s, cfx(c)) }, listOf(sub))
        return sub
    }

    override fun consume(source: T, ctx: C): Unit {
        return fx(source, ctx)
    }


    companion object Factory {
        fun <T, C> of(field: Field, fx: (T, C) -> Unit): SimpleConv<T, C> {
            return SimpleConv(field, fx)
        }

        fun <T, C> repeating(conv: Convertor<Collection<T>, C>): Convertor<T, C> {
            val sub = SimpleConv<T, C>(conv)
            conv.complete { sList: Collection<T>, c: C ->
                sList.forEach { s -> sub.consume(s, c) }
            }
            return sub
        }
    }
}
