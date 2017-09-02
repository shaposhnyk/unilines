package com.shaposhnyk.univerter

interface TriConsumer<T, U, V> {
    fun accept(t: T, u: U, v: V)
}