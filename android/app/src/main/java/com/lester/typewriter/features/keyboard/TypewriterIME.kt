package com.lester.typewriter.features.keyboard

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo

/**
 * The keyboard service Android talks to. It decides what each key does to the app's
 * text box; [TypewriterKeyboardView] does the drawing and the touch handling.
 */
class TypewriterIME : InputMethodService(), TypewriterKeyboardView.Listener {

    private var keyboard: TypewriterKeyboardView? = null
    private val shift = ShiftState()

    override fun onCreateInputView(): View =
        TypewriterKeyboardView(this, this).also { keyboard = it }

    // In landscape, keep the app visible above the keyboard instead of Android's full-screen box.
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!restarting) shift.reset()
        updateAutoCaps()
        keyboard?.logKeyCentresForTour()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        keyboard?.cancelTouch()
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        updateAutoCaps() // the cursor moved, for example the user tapped elsewhere in the text
    }

    override fun onKey(key: KeySpec) {
        val ic = currentInputConnection ?: return
        when (key.kind) {
            KeyKind.LETTER -> {
                ic.commitText(if (shift.isUpperCase) key.label else key.output, 1)
                shift.onTyped()
            }
            KeyKind.SYMBOL, KeyKind.SPACE -> {
                ic.commitText(key.output, 1)
                shift.onTyped()
            }
            // A delete key event lets the app remove a whole emoji, or the whole selection.
            KeyKind.DELETE -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            // Search, Send, Go, Next... if the box has an action; otherwise a new line.
            KeyKind.ENTER -> if (!sendDefaultEditorAction(true)) ic.commitText("\n", 1)
            KeyKind.SHIFT -> shift.onShiftTap(SystemClock.uptimeMillis())
        }
        if (key.kind != KeyKind.SHIFT) updateAutoCaps()
        keyboard?.shiftMode = shift.mode
    }

    /**
     * Ask the app whether a capital belongs at the cursor. Apps only say yes for boxes
     * that want sentence capitals, so passwords, emails and links stay lower case.
     */
    private fun updateAutoCaps() {
        val info = currentInputEditorInfo
        val ic = currentInputConnection
        val wanted = info != null && ic != null &&
            info.inputType != InputType.TYPE_NULL &&
            ic.getCursorCapsMode(info.inputType) != 0
        shift.applyAutoCaps(wanted)
        keyboard?.shiftMode = shift.mode
    }
}
