package com.lester.typewriter.features.keyboard

enum class ShiftMode { OFF, ONCE, LOCKED }

/**
 * Shift, like a typewriter's shift and shift lock:
 *  - tap shift: the next letter is a capital (ONCE)
 *  - tap shift twice quickly: every letter is a capital (LOCKED) until shift is tapped again
 *  - the app can ask for a capital at the start of a sentence (auto-caps)
 *
 * Pure logic (the time is passed in), so it is unit-tested on the JVM.
 */
class ShiftState(private val doubleTapMillis: Long = 350L) {

    var mode: ShiftMode = ShiftMode.OFF
        private set

    val isUpperCase: Boolean get() = mode != ShiftMode.OFF

    private var lastTap = NO_TAP
    private var fromAutoCaps = false

    fun onShiftTap(nowMillis: Long) {
        val quickSecondTap = lastTap != NO_TAP && nowMillis - lastTap <= doubleTapMillis
        when {
            mode == ShiftMode.LOCKED -> {
                mode = ShiftMode.OFF
                lastTap = NO_TAP
            }
            quickSecondTap -> {
                mode = ShiftMode.LOCKED
                lastTap = NO_TAP
            }
            mode == ShiftMode.OFF -> {
                mode = ShiftMode.ONCE
                lastTap = nowMillis
            }
            else -> { // ONCE: tapping again cancels it
                mode = ShiftMode.OFF
                lastTap = nowMillis
            }
        }
        fromAutoCaps = false
    }

    /** A character was typed: a one-off capital is used up. Shift lock stays on. */
    fun onTyped() {
        if (mode == ShiftMode.ONCE) mode = ShiftMode.OFF
        fromAutoCaps = false
        lastTap = NO_TAP
    }

    /**
     * The app says whether a capital belongs at the cursor (start of a sentence).
     * Only turns off a capital that auto-caps itself turned on, never one the user chose.
     */
    fun applyAutoCaps(capitalWanted: Boolean) {
        if (mode == ShiftMode.LOCKED) return
        if (capitalWanted && mode == ShiftMode.OFF) {
            mode = ShiftMode.ONCE
            fromAutoCaps = true
        } else if (!capitalWanted && mode == ShiftMode.ONCE && fromAutoCaps) {
            mode = ShiftMode.OFF
            fromAutoCaps = false
        }
    }

    fun reset() {
        mode = ShiftMode.OFF
        lastTap = NO_TAP
        fromAutoCaps = false
    }

    private companion object {
        const val NO_TAP = -1L
    }
}
