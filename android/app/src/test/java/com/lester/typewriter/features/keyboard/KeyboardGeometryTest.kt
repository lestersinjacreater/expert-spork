package com.lester.typewriter.features.keyboard

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class KeyboardGeometryTest {

    private val g = KeyboardGeometry(KeyboardLayout.qwerty, width = 1080f)

    @Test
    fun theCentreOfEveryKeySelectsThatKey() {
        for (k in g.keys) assertSame(k.spec.id, k, g.keyAt(k.cx, k.cy))
    }

    @Test
    fun aTapInTheGapBetweenRoundKeysStillHitsANeighbour() {
        val q = g.key("q")!!
        val w = g.key("w")!!
        val hit = g.keyAt((q.cx + w.cx) / 2f, q.cy)
        assertTrue(hit === q || hit === w)
    }

    @Test
    fun thereAreNoDeadSpotsBelowThePaper() {
        var y = g.paperHeight
        while (y < g.height) {
            var x = 0f
            while (x < g.width) {
                assertNotNull("dead spot at $x,$y", g.keyAt(x, y))
                x += 9f
            }
            y += 9f
        }
    }

    @Test
    fun touchesOnThePaperStripAreIgnored() {
        assertNull(g.keyAt(g.width / 2f, g.paperHeight / 2f))
    }

    @Test
    fun theRoundEndsOfTheSpaceBarBelongToTheSpaceBar() {
        val space = g.key("space")!!
        val nearLeftEnd = space.cx - space.w / 2f + space.h * 0.3f
        val nearRightEnd = space.cx + space.w / 2f - space.h * 0.3f
        assertSame(space, g.keyAt(nearLeftEnd, space.cy))
        assertSame(space, g.keyAt(nearRightEnd, space.cy))
    }

    @Test
    fun everyKeyFitsInsideTheKeyboard() {
        for (k in g.keys) {
            assertTrue(k.spec.id, k.cx - k.w / 2f >= 0f)
            assertTrue(k.spec.id, k.cx + k.w / 2f <= g.width)
            assertTrue(k.spec.id, k.cy + k.h / 2f <= g.height)
        }
    }

    @Test
    fun keysInARowDoNotOverlap() {
        for (row in g.keys.groupBy { it.row }.values) {
            for (i in 0 until row.size - 1) {
                val a = row[i]
                val b = row[i + 1]
                assertTrue("${a.spec.id}/${b.spec.id}", abs(b.cx - a.cx) >= (a.w + b.w) / 2f - 0.01f)
            }
        }
    }

    @Test
    fun inLandscapeTheHeightIsCapped() {
        val wide = KeyboardGeometry(KeyboardLayout.qwerty, width = 2400f, maxHeight = 540f)
        assertTrue(wide.height <= 540.5f)
        assertTrue(wide.keys.all { it.cx > 0f && it.cx < 2400f })
    }
}
