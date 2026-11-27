package com.lester.typewriter.features.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import com.lester.typewriter.BuildConfig
import com.lester.typewriter.core.theme.Machines
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The keyboard on screen: one View that paints every key itself ([KeyArt]) and works
 * out which key a finger means ([KeyboardGeometry.keyAt]).
 *
 * Letters type when the finger lifts (so a slide to a neighbouring key changes the
 * choice); delete acts as soon as it is pressed and repeats while held.
 */
@SuppressLint("ViewConstructor")
class TypewriterKeyboardView(
    context: Context,
    private val listener: Listener,
) : View(context) {

    fun interface Listener {
        fun onKey(key: KeySpec)
    }

    var shiftMode: ShiftMode = ShiftMode.OFF
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val art = KeyArt(Machines.Classic)
    private val feedback = KeyFeedback(this)
    private var geometry: KeyboardGeometry? = null
    private var pressedKey: KeyboardGeometry.PlacedKey? = null
    private var activePointer = NO_POINTER

    private val repeatHandler = Handler(Looper.getMainLooper())
    private val repeatDelete = object : Runnable {
        override fun run() {
            val key = pressedKey ?: return
            if (key.spec.kind != KeyKind.DELETE) return
            listener.onKey(key.spec)
            repeatHandler.postDelayed(this, REPEAT_EVERY_MS)
        }
    }

    init {
        // Android 15+ draws keyboards edge to edge: keep the keys above the navigation bar.
        setOnApplyWindowInsetsListener { _, insets ->
            val bottom = if (Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(WindowInsets.Type.systemBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            if (bottom != paddingBottom) {
                setPadding(0, 0, 0, bottom)
                requestLayout()
            }
            insets
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        if (width <= 0) {
            setMeasuredDimension(0, 0)
            return
        }
        setMeasuredDimension(width, ceil(geometryFor(width).height).toInt() + paddingBottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0) return
        art.prepare(geometryFor(w))
        logKeyCentresForTour()
    }

    private fun geometryFor(width: Int): KeyboardGeometry {
        geometry?.let { if (it.width == width.toFloat()) return it }
        val maxHeight = resources.displayMetrics.heightPixels * MAX_SHARE_OF_SCREEN
        return KeyboardGeometry(KeyboardLayout.qwerty, width.toFloat(), maxHeight).also { geometry = it }
    }

    override fun onDraw(canvas: Canvas) {
        val g = geometry ?: return
        val pressed = pressedKey
        art.drawBody(canvas, g, height.toFloat(), pressed)
        for (k in g.keys) {
            // Shift lock stays down, like the shift-lock key on a real typewriter.
            val down = k === pressed || (k.spec.kind == KeyKind.SHIFT && shiftMode == ShiftMode.LOCKED)
            art.drawKey(canvas, k, down, shiftMode)
        }
        if (pressed != null) {
            val stamp = stampFor(pressed.spec)
            if (stamp != null) art.drawStamp(canvas, g, pressed, stamp)
        }
    }

    /** What a pressed key would print on the paper (no allocation: the strings already exist). */
    private fun stampFor(spec: KeySpec): String? = when (spec.kind) {
        KeyKind.LETTER -> if (shiftMode == ShiftMode.OFF) spec.output else spec.label
        KeyKind.SYMBOL -> spec.output
        else -> null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val g = geometry ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> press(g, event, event.actionIndex)
            MotionEvent.ACTION_POINTER_DOWN -> {
                // A second finger lands before the first lifts (fast typing): finish the first key now.
                release(commit = true)
                press(g, event, event.actionIndex)
            }
            MotionEvent.ACTION_MOVE -> slide(g, event)
            MotionEvent.ACTION_POINTER_UP ->
                if (event.getPointerId(event.actionIndex) == activePointer) release(commit = true)
            MotionEvent.ACTION_UP -> release(commit = true)
            MotionEvent.ACTION_CANCEL -> release(commit = false)
        }
        return true
    }

    private fun press(g: KeyboardGeometry, event: MotionEvent, index: Int) {
        val key = g.keyAt(event.getX(index), event.getY(index)) ?: return
        pressedKey = key
        activePointer = event.getPointerId(index)
        feedback.onPress(key.spec.kind)
        if (key.spec.kind == KeyKind.DELETE) {
            listener.onKey(key.spec)
            repeatHandler.postDelayed(repeatDelete, REPEAT_START_MS)
        }
        invalidate()
    }

    private fun slide(g: KeyboardGeometry, event: MotionEvent) {
        val current = pressedKey ?: return
        if (current.spec.kind == KeyKind.DELETE) return // keep deleting while held
        val index = event.findPointerIndex(activePointer)
        if (index < 0) return
        val key = g.keyAt(event.getX(index), event.getY(index)) ?: return
        if (key !== current && key.spec.kind != KeyKind.DELETE) {
            pressedKey = key
            invalidate()
        }
    }

    private fun release(commit: Boolean) {
        val key = pressedKey ?: return
        repeatHandler.removeCallbacks(repeatDelete)
        pressedKey = null
        activePointer = NO_POINTER
        if (commit && key.spec.kind != KeyKind.DELETE) listener.onKey(key.spec)
        invalidate()
    }

    /** The keyboard is hiding: drop any key still held, without typing it. */
    fun cancelTouch() = release(commit = false)

    override fun onDetachedFromWindow() {
        repeatHandler.removeCallbacks(repeatDelete)
        super.onDetachedFromWindow()
    }

    /**
     * Debug builds only: print each key's centre on screen, so the virtual-phone tour can
     * tap real keys. This is positions only, never anything typed (CLAUDE.md, rule 2).
     */
    fun logKeyCentresForTour() {
        if (!BuildConfig.DEBUG) return
        post {
            val g = geometry ?: return@post
            val origin = IntArray(2)
            getLocationOnScreen(origin)
            for (k in g.keys) {
                val x = (origin[0] + k.cx).roundToInt()
                val y = (origin[1] + k.cy).roundToInt()
                Log.d(TOUR_LOG_TAG, "key ${k.spec.id} $x $y")
            }
        }
    }

    private companion object {
        const val NO_POINTER = -1
        const val REPEAT_START_MS = 400L
        const val REPEAT_EVERY_MS = 60L
        const val MAX_SHARE_OF_SCREEN = 0.5f // in landscape the keyboard never takes more than half
        const val TOUR_LOG_TAG = "TWKEYS"
    }
}
