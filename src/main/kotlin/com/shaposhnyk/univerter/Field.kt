package com.shaposhnyk.univerter

/**
 * Simple mapping field, with factory object
 */
interface Field {
    fun internalName(): String

    fun externalName(): String

    fun public(): Boolean

    fun hasFilter(): Boolean

    fun description(): String

    companion object Factory {
        data class SimpleField(
                val internal: String,
                val external: String) : Field {

            override fun public(): Boolean = true

            override fun hasFilter(): Boolean = false

            override fun description(): String = ""

            override fun internalName(): String = this.internal

            override fun externalName(): String = this.external
        }

        data class DefaultField(
                val internal: String,
                val external: String,
                val isPublic: Boolean = true,
                val hasFilter: Boolean = false,
                val description: String = "") : Field {

            override fun public(): Boolean = this.isPublic

            override fun hasFilter(): Boolean = this.hasFilter

            override fun description(): String = this.description

            override fun internalName(): String = this.internal

            override fun externalName(): String = this.external
        }

        fun of(name: String): Field = SimpleField(name, name)

        fun of(intr: String, extr: String): Field = SimpleField(intr, extr)

        fun privateOf(f: Field): Field = DefaultField(f.internalName(), f.externalName(), false)
    }

}