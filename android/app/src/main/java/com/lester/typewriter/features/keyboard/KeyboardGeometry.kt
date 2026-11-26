package com.lester.typewriter.features.keyboard

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Where every key sits, in pixels, for a keyboard of a given width.
 * Pure maths with no Android types, so it runs in plain JVM unit tests.
 *
 * Sizes are worked out in "units": one unit is the slot of one letter key,
 * and a row of ten letters fills the width (minus a small margin each side).
 * In landscape the unit is capped by [maxHeight], so the keyboard never
 * takes more than that much of the screen.
 */
class KeyboardGeometry(
    val rows: List<List<KeySpec>>,
    val width: Float,
    maxHeight: Float = Float.MAX_VALUE,
) {
    /** A key placed on the keyboard. [w] and [h] are the drawn cap, not the touch area. */
    class PlacedKey(
        val spec: KeySpec,
        val row: Int,
        val cx: Float,
        val cy: Float,
        val w: Float,
        val h: Float,
    )

    val unit: Float
    val paperHeight: Float

    /** The y of the bar the typebar rods hang from. */
    val segmentY: Float
    val height: Float
    val keys: List<PlacedKey>

    init {
        require(width > 0f) { "width must be positive" }
        val widthUnit = width * (1f - 2 * SIDE_MARGIN) / UNITS_PER_ROW
        unit = min(widthUnit, maxHeight / heightInUnits(rows.size))
        paperHeight = unit * PAPER
        segmentY = paperHeight + unit * GAP * 0.35f
        val firstRowCentre = paperHeight + unit * GAP + unit * ROW_PITCH / 2f
        val capSize = unit * CAP
        val placed = ArrayList<PlacedKey>()
        rows.forEachIndexed { r, row ->
            val rowWidth = row.sumOf { it.units.toDouble() }.toFloat() * unit
            var x = (width - rowWidth) / 2f
            val cy = firstRowCentre + r * unit * ROW_PITCH
            for (spec in row) {
                val slot = spec.units * unit
                placed += PlacedKey(spec, r, x + slot / 2f, cy, slot - (unit - capSize), capSize)
                x += slot
            }
        }
        keys = placed
        height = unit * heightInUnits(rows.size)
    }

    /**
     * The key a touch at (x, y) belongs to: the one whose cap edge is closest.
     * Taps in the gaps between round caps still count, so there are no dead spots.
     * Touches on the paper strip return null.
     */
    fun keyAt(x: Float, y: Float): PlacedKey? {
        if (y < paperHeight) return null
        var best: PlacedKey? = null
        var bestDistance = Float.MAX_VALUE
        for (k in keys) {
            val d = distanceToCap(k, x, y)
            if (d < bestDistance) {
                bestDistance = d
                best = k
            }
        }
        return best
    }

    fun key(id: String): PlacedKey? = keys.firstOrNull { it.spec.id == id }

    companion object {
        const val UNITS_PER_ROW = 10f
        const val SIDE_MARGIN = 0.02f // of the width, on each side
        const val CAP = 0.86f // cap height (and round-cap diameter), in units
        const val ROW_PITCH = 1.1f // distance between row centres
        const val PAPER = 0.62f // the paper strip along the top
        const val GAP = 0.42f // between the paper and the first row: the rods show here
        const val BOTTOM = 0.2f // below the last row

        fun heightInUnits(rowCount: Int): Float = PAPER + GAP + rowCount * ROW_PITCH + BOTTOM

        /** Distance from a point to the edge of a cap (round or pill); 0 or less means inside it. */
        fun distanceToCap(k: PlacedKey, x: Float, y: Float): Float {
            val half = max(0f, (k.w - k.h) / 2f)
            val nearestX = x.coerceIn(k.cx - half, k.cx + half)
            return hypot(x - nearestX, y - k.cy) - k.h / 2f
        }
    }
}
