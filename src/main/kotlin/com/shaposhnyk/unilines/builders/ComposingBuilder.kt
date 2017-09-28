package com.shaposhnyk.unilines.builders

import com.shaposhnyk.unilines.UBiPipeline

interface ComposingBuilder<in TIN, CIN, TOUT, COUT> {
    /**
     * Adds a field to the builder
     */
    fun field(converter: UBiPipeline<TOUT, COUT>): ComposingBuilder<TIN, CIN, TOUT, COUT>

    /**
     * Adds fields to the builder
     */
    fun fields(converters: Collection<UBiPipeline<TOUT, COUT>>): ComposingBuilder<TIN, CIN, TOUT, COUT>

    /**
     * Terminal operation, constructing a converter from the builder
     */
    fun build(): UBiPipeline<TIN, CIN>
}