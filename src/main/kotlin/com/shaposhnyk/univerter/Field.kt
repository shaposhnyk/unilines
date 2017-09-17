package com.shaposhnyk.univerter

/**
 * Simple mapping field, with factory object
 */
interface Field {

    fun internalName(): String

    fun externalName(): String

    fun isPublic(): Boolean

    fun hasFilter(): Boolean

    fun description(): String

    /**
     * Factory companion object
     */
    companion object Factory {
        /**
         * Simplest mapping field (isPublic, non-filter, w/o description)
         */
        data class SimpleField(
                val internal: String,
                val external: String) : Field {

            override fun isPublic(): Boolean = true

            override fun hasFilter(): Boolean = false

            override fun description(): String = ""

            override fun internalName(): String = this.internal

            override fun externalName(): String = this.external
        }

        data class DefaultField(
                val internal: String,
                val external: String,
                val public: Boolean = true,
                val hasFilter: Boolean = false,
                val description: String = "") : Field {

            override fun isPublic(): Boolean = this.public

            override fun hasFilter(): Boolean = this.hasFilter

            override fun description(): String = this.description

            override fun internalName(): String = this.internal

            override fun externalName(): String = this.external
        }

        /**
         * @name - name to be used as internal and external names
         * @return new field with internal and external names same
         */
        fun of(name: String): Field = SimpleField(name, name)

        /**
         * @return new field with given internal and external names
         */
        fun of(internalName: String, externalName: String): Field = SimpleField(internalName, externalName)

        /**
         * @return new field, basing on a given one with isPublic() set to false
         */
        fun privateOf(f: Field): Field {
            if (!f.isPublic()) {
                return f
            }
            return DefaultField(f.internalName(), f.externalName(), false, f.hasFilter(), f.description())
        }

        /**
         * @return new field, basing on a given one with hasFilter() set to true
         */
        fun filterOf(f: Field): Field {
            if (f.hasFilter()) {
                return f
            }
            return DefaultField(f.internalName(), f.externalName(), f.isPublic(), true, f.description())
        }

        /**
         * @return new field, basing on a given one with a new description
         */
        fun withDescription(f: Field, descr: String): Field {
            return DefaultField(f.internalName(), f.externalName(), f.isPublic(), f.hasFilter(), descr)
        }
    }
}