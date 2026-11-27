package com.lester.typewriter.features.onboarding

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

/** Whether Typewriter is switched on in Settings, and whether it is the keyboard in use. */
data class KeyboardStatus(val enabled: Boolean, val selected: Boolean) {

    companion object {
        fun read(context: Context): KeyboardStatus {
            val imm = context.getSystemService(InputMethodManager::class.java)
            val enabled = imm?.enabledInputMethodList.orEmpty()
                .any { it.packageName == context.packageName }
            val current = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD,
            )
            val selected = current != null &&
                ComponentName.unflattenFromString(current)?.packageName == context.packageName
            return KeyboardStatus(enabled = enabled, selected = enabled && selected)
        }
    }
}
