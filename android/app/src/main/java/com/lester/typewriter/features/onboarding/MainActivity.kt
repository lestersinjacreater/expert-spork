package com.lester.typewriter.features.onboarding

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lester.typewriter.BuildConfig

/** The app you open from the home screen: switch the keyboard on, pick it, try it. */
class MainActivity : ComponentActivity() {

    private var status by mutableStateOf(KeyboardStatus(enabled = false, selected = false))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WelcomeScreen(
                status = status,
                version = BuildConfig.VERSION_NAME,
                onTurnOn = ::openKeyboardSettings,
                onPick = ::openKeyboardPicker,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    // Coming back from Settings, or closing the keyboard picker, gives the window focus again.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refresh()
    }

    private fun refresh() {
        status = KeyboardStatus.read(this)
    }

    private fun openKeyboardSettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }

    private fun openKeyboardPicker() {
        getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
    }
}
