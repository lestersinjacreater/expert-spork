package com.lester.typewriter.features.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {

    private val keys = KeyboardLayout.qwerty.flatten()

    @Test
    fun hasEveryLetterExactlyOnce() {
        val letters = keys.filter { it.kind == KeyKind.LETTER }.map { it.output }
        assertEquals(26, letters.size)
        assertEquals(('a'..'z').map { it.toString() }.toSet(), letters.toSet())
    }

    @Test
    fun legendsAreCapitalsOfWhatTheKeyTypes() {
        for (k in keys.filter { it.kind == KeyKind.LETTER }) {
            assertEquals(k.output.uppercase(), k.label)
        }
    }

    @Test
    fun keyIdsAreUnique() {
        assertEquals(keys.size, keys.map { it.id }.toSet().size)
    }

    @Test
    fun noRowIsWiderThanTenLetterKeys() {
        for (row in KeyboardLayout.qwerty) {
            assertTrue(row.sumOf { it.units.toDouble() } <= 10.0)
        }
    }

    @Test
    fun hasTheControlKeys() {
        for (id in listOf("shift", "delete", "space", "enter")) {
            assertTrue("missing $id", keys.any { it.id == id })
        }
    }
}
