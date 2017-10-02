package com.shaposhnyk.unilines.builders

import com.shaposhnyk.unilines.UBiPipeline

interface ComposingBuilder<in TIN, CIN, TOUT, COUT> {
    /**
     * Current list of fields
     */
    fun fields(): List<UBiPipeline<*, *>>

    /**
     * Adds a field to the builder
     */
    fun field(converter: UBiPipeline<TOUT, COUT>): ComposingBuilder<TIN, CIN, TOUT, COUT>

    /**
     * Terminal operation, constructing a downstream from the builder
     */
    fun build(): UBiPipeline<TIN, CIN>

    /**
     * Adds fields to the builder
     */
    fun fields(converters: Collection<UBiPipeline<TOUT, COUT>>): ComposingBuilder<TIN, CIN, TOUT, COUT> {
        converters.forEach { c -> field(c) }
        return this
    }

    fun pipeTo(downstream: UBiPipeline<TOUT, COUT>): UBiPipeline<TIN, CIN> {
        if (!fields().isEmpty()) {
            throw IllegalStateException("pipeTo is mutually exclusive with field()")
        }
        return field(downstream)
                .build()
    }
}