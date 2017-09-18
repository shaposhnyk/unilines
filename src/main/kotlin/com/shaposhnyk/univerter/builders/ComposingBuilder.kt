package com.shaposhnyk.univerter.builders

import com.shaposhnyk.univerter.Converter

interface ComposingBuilder<TIN, CIN, TOUT, COUT> {
    /**
     * Adds a field to the builder
     */
    fun field(converter: Converter<TOUT, COUT>): ComposingBuilder<TIN, CIN, TOUT, COUT>

    /**
     * Adds fields to the builder
     */
    fun fields(converters: Collection<Converter<TOUT, COUT>>): ComposingBuilder<TIN, CIN, TOUT, COUT>

    /**
     * Terminal operation, constructing a converter from the builder
     */
    fun build(): Converter<TIN, CIN>
}