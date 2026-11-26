package com.lester.typewriter.features.keyboard

import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * The feel of a key press: a short vibration and a click.
 * For now the click is Android's own keyboard sound; recorded typewriter sounds replace it later.
 */
class KeyFeedback(private val view: View) {

    private val audio: AudioManager? = view.context.getSystemService(AudioManager::class.java)

    fun onPress(kind: KeyKind) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        val am = audio ?: return
        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return // silent or vibrate: stay quiet
        val sound = when (kind) {
            KeyKind.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
            KeyKind.DELETE -> AudioManager.FX_KEYPRESS_DELETE
            KeyKind.ENTER -> AudioManager.FX_KEYPRESS_RETURN
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        am.playSoundEffect(sound, DEFAULT_VOLUME)
    }

    private companion object {
        const val DEFAULT_VOLUME = -1f // Android's default keyboard-click volume
    }
}
