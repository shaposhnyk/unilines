package com.shaposhnyk.univerter

/**
 * Consumer which takes 3 inputs.
 * Often used to specify a function on a Field, an inputObject, and a working Context
 */
interface TriConsumer<in T, in U, in V> {
    fun accept(t: T, u: U, v: V)
}