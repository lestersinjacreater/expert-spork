package com.lester.typewriter.core.theme

/**
 * The colours of one "machine" (a keyboard theme), as plain ARGB values.
 * Hardcoded on purpose: a new theme is just a new value here, no image files.
 */
data class MachinePalette(
    val plate: Int, // the body the keys sit on
    val paper: Int, // the paper strip along the top
    val paperEdge: Int, // the shadow line under the paper
    val ink: Int, // letters stamped on the paper
    val rod: Int, // typebar rods behind the keys
    val rodActive: Int, // the rod of the key being pressed
    val segment: Int, // the bar the rods hang from
    val rimLight: Int, // chrome rim, top
    val rimDark: Int, // chrome rim, bottom
    val groove: Int, // the dark gap between rim and cap
    val capHighlight: Int, // black enamel cap, lit side
    val capBody: Int, // black enamel cap, shadow side
    val redHighlight: Int, // red backspace enamel, lit side
    val redBody: Int, // red backspace enamel, shadow side
    val legend: Int, // letters printed on the caps
    val gloss: Int, // the soft reflection on each cap
    val shadowSoft: Int, // wide, faint shadow under a key
    val shadowHard: Int, // tight, darker shadow under a key
)

object Machines {
    /** The mockup: light grey body, black keys with chrome rims, cream letters, red backspace. */
    val Classic = MachinePalette(
        plate = 0xFFE4E4E7.toInt(),
        paper = 0xFFF6F1E4.toInt(),
        paperEdge = 0xFFCFC8B6.toInt(),
        ink = 0xFF1E1B18.toInt(),
        rod = 0xFF4A4A50.toInt(),
        rodActive = 0xFFC9CACF.toInt(),
        segment = 0xFF2E2E33.toInt(),
        rimLight = 0xFFF4F4F6.toInt(),
        rimDark = 0xFF6A6A71.toInt(),
        groove = 0xFF202024.toInt(),
        capHighlight = 0xFF4C4C52.toInt(),
        capBody = 0xFF0E0E10.toInt(),
        redHighlight = 0xFFB02C42.toInt(),
        redBody = 0xFF570B17.toInt(),
        legend = 0xFFEFE6D2.toInt(),
        gloss = 0x2EFFFFFF,
        shadowSoft = 0x22000000,
        shadowHard = 0x44000000,
    )
}
