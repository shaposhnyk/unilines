package com.shaposhnyk.unilines

/**
 * Consumer which takes 3 inputs.
 * Often used to specify a function on a UField, an inputObject, and a working Context
 */
interface UTriConsumer<in T, in U, in V> {
    fun accept(t: T, u: U, v: V)
}