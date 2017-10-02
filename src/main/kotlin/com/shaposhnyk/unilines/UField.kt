package com.shaposhnyk.unilines

/**
 * Mapping Field interface, with a factory companion object
 */
interface UField {

    /**
     * @return non-null name of the field in the current system
     */
    fun internalName(): String

    /**
     * @return non-null name of the field in an external system
     */
    fun externalName(): String

    /**
     * @return true if field is public
     */
    fun isPublic(): Boolean

    /**
     * @return true if field is used as filter
     */
    fun hasFilter(): Boolean

    /**
     * @return non-null description of a field mapping
     */
    fun description(): String = ""

    /*
     * builder style setters
     */

    fun isPublic(p: Boolean) = if (p) publicOf(this) else privateOf(this)

    fun filtering(f: Boolean) = if (f) filterOf(this) else fieldOf(this)

    fun description(descr: String) = withDescription(this, descr)


    /**
     * Factory companion object
     */
    companion object Factory {
        /**
         * Simplest mapping field (isPublic, non-filter, w/o description)
         */
        data class SimpleImmutableField(
                private val internal: String,
                private val external: String) : UField {

            override fun isPublic(): Boolean = true

            override fun hasFilter(): Boolean = false

            override fun description(): String = ""

            override fun internalName(): String = this.internal

            override fun externalName(): String = this.external

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other is SimpleImmutableField) {
                    return internal == other.internal && external == other.external
                } else if (other is UField) {
                    return other.isPublic()
                            && !other.hasFilter()
                            && other.description().isEmpty()
                            && internal == other.internalName()
                            && external == other.externalName()
                }
                return false
            }
        }

        /**
         * Immutable mapping field with all fields
         */
        data class ImmutableField(
                private val internal: String,
                private val external: String,
                private val public: Boolean = true,
                private val hasFilter: Boolean = false,
                private val description: String = "") : UField {

            override fun isPublic(): Boolean = this.public

            override fun hasFilter(): Boolean = this.hasFilter

            override fun description(): String = this.description

            override fun internalName(): String = this.internal

            override fun externalName(): String = this.external

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other is SimpleImmutableField && public && !hasFilter && description.isEmpty()) {
                    return internal == other.internalName() && external == other.externalName()
                }
                return areEqual(this, other)
            }

            override fun hashCode(): Int {
                // Hope this will be optimized by compiler
                // Anyway this could be faster than calculating hashCode() on 5 fields
                // But if not this should be rewritten
                return SimpleImmutableField(internal, external).hashCode()
            }
        }

        /**
         * @return true if second object is a UField and its properties are the same
         */
        fun areEqual(first: UField, second: Any?): Boolean {
            if (second is UField) {
                return first.internalName() == second.internalName()
                        && first.externalName() == second.externalName()
                        && first.isPublic() == second.isPublic()
                        && first.hasFilter() == second.hasFilter()
                        && first.description() == second.description()
            }
            return false
        }

        /**
         * @name - name to be used as internal and external names
         * @return new field with internal and external names same
         */
        fun of(name: String): UField = SimpleImmutableField(name, name)

        /**
         * @return new field with given internal and external names
         */
        fun of(internalName: String, externalName: String): UField = SimpleImmutableField(internalName, externalName)

        /**
         * Creates a new field, taking parameters from the given one and setting isPublic() flag to false
         * @return new field, basing on a given one with isPublic() set to false
         */
        fun privateOf(f: UField): UField {
            if (!f.isPublic()) {
                return f
            }
            return ImmutableField(f.internalName(), f.externalName(), false, f.hasFilter(), f.description())
        }

        /**
         * Creates a new field, taking parameters from the given one and setting isPublic() flag to true
         * @return new field, basing on a given one with isPublic() set to false
         */
        fun publicOf(f: UField): UField {
            if (f.isPublic()) {
                return f
            }
            return ImmutableField(f.internalName(), f.externalName(), true, f.hasFilter(), f.description())
        }

        /**
         * Creates a new field, taking parameters from the given one and setting filter flag to true
         * @return new field, basing on a given one with hasFilter() set to true
         */
        fun filterOf(f: UField): UField {
            if (f.hasFilter()) {
                return f
            }
            return ImmutableField(f.internalName(), f.externalName(), f.isPublic(), true, f.description())
        }

        /**
         * Creates a new field, taking parameters from the given one and setting filter flag to false
         * @return new field, basing on a given one with hasFilter() set to true
         */
        fun fieldOf(f: UField): UField {
            if (f.hasFilter()) {
                return f
            }
            return ImmutableField(f.internalName(), f.externalName(), f.isPublic(), false, f.description())
        }

        /**
         * Creates a new field, taking parameters from the given one and adding the description
         * @return new field, basing on a given one with a new description
         */
        fun withDescription(f: UField, description: String): UField {
            return ImmutableField(f.internalName(), f.externalName(), f.isPublic(), f.hasFilter(), description)
        }

        /**
         * Empty field
         */
        fun empty() = ImmutableField("", "", false, false, "dummy")
    }
}