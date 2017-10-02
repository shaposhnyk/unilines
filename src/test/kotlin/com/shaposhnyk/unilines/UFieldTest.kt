package com.shaposhnyk.unilines

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UFieldTest {

    @Test
    fun testHashCode() {
        val simple = UField.of("some").hashCode()
        val immutable = UField.Factory.ImmutableField("some", "some").hashCode()
        assertEquals(simple, immutable)
    }

    @Test
    fun testHashCode2() {
        val simple = UField.of("some", "someone").hashCode()
        val immutable = UField.Factory.ImmutableField("some", "someone").hashCode()
        assertEquals(simple, immutable)
    }

    @Test
    fun testEquals() {
        val simple = UField.of("some")
        val immutable = UField.Factory.ImmutableField("some", "some")
        assertEquals(simple, immutable)
    }

    @Test
    fun testEquals2() {
        val simple = UField.of("some", "someone")
        val immutable = UField.Factory.ImmutableField("some", "someone")
        assertEquals(simple, immutable)
    }

    @Test
    fun testEquals3() {
        val simple = UField.of("some", "someone")
        val immutable = UField.Factory.ImmutableField("some", "someone", false)
        assertNotEquals(simple, immutable)
    }

    @Test
    fun testEquals4() {
        val simple = UField.of("some", "someone").isPublic(false).isPublic(true)
        val immutable = UField.Factory.ImmutableField("some", "someone", false)
        assertNotEquals(simple, immutable)
    }

    @Test
    fun testReferentialEquals() {
        val simple = UField.of("some", "someone")
        val simplePublic = simple.isPublic(true)
        assert(simple === simplePublic)
    }
}
