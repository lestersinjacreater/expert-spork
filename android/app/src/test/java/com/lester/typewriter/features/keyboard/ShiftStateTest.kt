package com.lester.typewriter.features.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftStateTest {

    private val shift = ShiftState(doubleTapMillis = 350L)

    @Test
    fun oneTapCapitalisesOnlyTheNextLetter() {
        shift.onShiftTap(0L)
        assertEquals(ShiftMode.ONCE, shift.mode)
        shift.onTyped()
        assertEquals(ShiftMode.OFF, shift.mode)
    }

    @Test
    fun twoQuickTapsLockAndAThirdUnlocks() {
        shift.onShiftTap(0L)
        shift.onShiftTap(200L)
        assertEquals(ShiftMode.LOCKED, shift.mode)
        shift.onTyped()
        shift.onTyped()
        assertEquals(ShiftMode.LOCKED, shift.mode)
        shift.onShiftTap(5_000L)
        assertEquals(ShiftMode.OFF, shift.mode)
    }

    @Test
    fun twoSlowTapsTurnShiftOnThenOff() {
        shift.onShiftTap(0L)
        shift.onShiftTap(1_000L)
        assertEquals(ShiftMode.OFF, shift.mode)
    }

    @Test
    fun aQuickTapAfterUnlockingDoesNotLockAgain() {
        shift.onShiftTap(0L)
        shift.onShiftTap(100L) // locked
        shift.onShiftTap(1_000L) // off
        shift.onShiftTap(1_100L)
        assertEquals(ShiftMode.ONCE, shift.mode)
    }

    @Test
    fun autoCapsTurnsOnAndOffByItself() {
        shift.applyAutoCaps(true)
        assertEquals(ShiftMode.ONCE, shift.mode)
        shift.applyAutoCaps(false)
        assertEquals(ShiftMode.OFF, shift.mode)
    }

    @Test
    fun autoCapsNeverCancelsACapitalTheUserChose() {
        shift.onShiftTap(0L)
        shift.applyAutoCaps(false)
        assertEquals(ShiftMode.ONCE, shift.mode)
    }

    @Test
    fun autoCapsLeavesShiftLockAlone() {
        shift.onShiftTap(0L)
        shift.onShiftTap(100L)
        shift.applyAutoCaps(false)
        assertEquals(ShiftMode.LOCKED, shift.mode)
    }

    @Test
    fun tappingShiftCancelsAnAutomaticCapitalAndADoubleTapStillLocks() {
        shift.applyAutoCaps(true)
        shift.onShiftTap(0L)
        assertEquals(ShiftMode.OFF, shift.mode)
        shift.onShiftTap(200L)
        assertEquals(ShiftMode.LOCKED, shift.mode)
    }
}
