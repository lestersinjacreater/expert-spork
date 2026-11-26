package com.lester.typewriter.features.keyboard

/** What a key does. */
enum class KeyKind { LETTER, SYMBOL, SHIFT, DELETE, SPACE, ENTER }

/**
 * One key.
 *
 * @param id stable name, also used by the virtual-phone tour to find the key ("q", "shift", "space")
 * @param label what is printed on the cap ("" for keys drawn as icons)
 * @param output the text it types (lower case for letters)
 * @param units width, where 1 unit is the slot of one letter key
 */
data class KeySpec(
    val id: String,
    val kind: KeyKind,
    val label: String,
    val output: String,
    val units: Float = 1f,
)

/** Keyboard layouts as plain data (no XML). Rows are listed top first. */
object KeyboardLayout {

    private fun letters(row: String): List<KeySpec> =
        row.map { KeySpec(it.toString(), KeyKind.LETTER, it.uppercaseChar().toString(), it.toString()) }

    /**
     * QWERTY. Every row is centred, so the shorter middle row sits half a key in,
     * like the staggered rows on a typewriter.
     */
    val qwerty: List<List<KeySpec>> = listOf(
        letters("qwertyuiop"),
        letters("asdfghjkl"),
        listOf(KeySpec("shift", KeyKind.SHIFT, "", "", units = 1.45f)) +
            letters("zxcvbnm") +
            listOf(KeySpec("delete", KeyKind.DELETE, "", "", units = 1.45f)),
        listOf(
            KeySpec("comma", KeyKind.SYMBOL, ",", ","),
            KeySpec("space", KeyKind.SPACE, "", " ", units = 5.6f),
            KeySpec("period", KeyKind.SYMBOL, ".", "."),
            KeySpec("enter", KeyKind.ENTER, "", "\n", units = 2f),
        ),
    )
}
