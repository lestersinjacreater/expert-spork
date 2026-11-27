package com.lester.typewriter.features.keyboard

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.lester.typewriter.core.theme.MachinePalette

/**
 * Paints the typewriter in code, with no image files: the plate, the paper strip, the
 * typebar rods and every key (shadow, chrome rim, enamel cap, gloss, legend).
 *
 * Every Paint, Path and Shader is built in [prepare] once the size is known. The draw
 * calls only reuse them, so nothing is allocated while drawing.
 */
class KeyArt(private val palette: MachinePalette) {

    private val platePaint = fill(palette.plate)
    private val paperPaint = fill(palette.paper)
    private val paperEdgePaint = fill(palette.paperEdge)
    private val segmentPaint = fill(palette.segment)
    private val rodPaint = stroke(palette.rod)
    private val rodActivePaint = stroke(palette.rodActive)
    private val shadowSoftPaint = fill(palette.shadowSoft)
    private val shadowHardPaint = fill(palette.shadowHard)
    private val rimPaint = fill(palette.rimDark)
    private val groovePaint = fill(palette.groove)
    private val roundCapPaint = fill(palette.capBody)
    private val pillCapPaint = fill(palette.capBody)
    private val redCapPaint = fill(palette.redBody)
    private val glossPaint = fill(palette.gloss)
    private val legendPaint = fill(palette.legend).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    private val iconStrokePaint = stroke(palette.legend).apply { strokeJoin = Paint.Join.ROUND }
    private val iconFillPaint = fill(palette.legend)
    private val stampPaint = fill(palette.ink).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val shiftArrow = Path()
    private val shiftLockBar = RectF()
    private val deleteTag = Path()
    private val deleteCross = Path()
    private val enterArrow = Path()
    private val rect = RectF()

    private var radius = 0f // half the cap height
    private var legendBaseline = 0f
    private var stampBaseline = 0f
    private var segmentHalf = 0f
    private var segmentLeft = 0f
    private var segmentRight = 0f
    private var paperEdge = 0f

    fun prepare(g: KeyboardGeometry) {
        val d = g.unit * KeyboardGeometry.CAP
        val r = d / 2f
        radius = r

        // The canvas is moved to each key's centre before drawing it, so these shaders work
        // in the key's own coordinates and one shader serves every key.
        // Chrome rim: bright at the top, dark at the bottom.
        rimPaint.shader =
            LinearGradient(0f, -r, 0f, r, palette.rimLight, palette.rimDark, Shader.TileMode.CLAMP)
        // Enamel: lit from the top left.
        roundCapPaint.shader = RadialGradient(
            -0.3f * r, -0.45f * r, 1.35f * r,
            palette.capHighlight, palette.capBody, Shader.TileMode.CLAMP,
        )
        pillCapPaint.shader =
            LinearGradient(0f, -r, 0f, r, palette.capHighlight, palette.capBody, Shader.TileMode.CLAMP)
        redCapPaint.shader =
            LinearGradient(0f, -r, 0f, r, palette.redHighlight, palette.redBody, Shader.TileMode.CLAMP)

        legendPaint.textSize = d * 0.46f
        legendBaseline = -(legendPaint.ascent() + legendPaint.descent()) / 2f

        stampPaint.textSize = g.paperHeight * 0.62f
        stampBaseline = g.paperHeight / 2f - (stampPaint.ascent() + stampPaint.descent()) / 2f

        rodPaint.strokeWidth = g.unit * 0.075f
        rodActivePaint.strokeWidth = g.unit * 0.075f
        iconStrokePaint.strokeWidth = d * 0.07f

        segmentHalf = g.unit * 0.07f
        val rodKeys = g.keys.filter { it.row < TYPEBAR_ROWS }
        segmentLeft = (rodKeys.minOfOrNull { it.cx } ?: 0f) - g.unit * 0.35f
        segmentRight = (rodKeys.maxOfOrNull { it.cx } ?: g.width) + g.unit * 0.35f
        paperEdge = g.unit * 0.035f

        buildIcons(d)
    }

    /** The plate, the paper strip and the typebar rods; the rod of [active] lights up. */
    fun drawBody(c: Canvas, g: KeyboardGeometry, fullHeight: Float, active: KeyboardGeometry.PlacedKey?) {
        c.drawRect(0f, 0f, g.width, fullHeight, platePaint)
        c.drawRect(0f, 0f, g.width, g.paperHeight, paperPaint)
        c.drawRect(0f, g.paperHeight - paperEdge, g.width, g.paperHeight, paperEdgePaint)
        for (k in g.keys) {
            if (k.row >= TYPEBAR_ROWS) continue
            c.drawLine(k.cx, g.segmentY, k.cx, k.cy, if (k === active) rodActivePaint else rodPaint)
        }
        rect.set(segmentLeft, g.segmentY - segmentHalf, segmentRight, g.segmentY + segmentHalf)
        c.drawRoundRect(rect, segmentHalf, segmentHalf, segmentPaint)
    }

    /** One key. [down] draws it pressed: a little smaller and lower, with a shorter shadow. */
    fun drawKey(c: Canvas, k: KeyboardGeometry.PlacedKey, down: Boolean, shift: ShiftMode) {
        val r = radius
        val halfW = k.w / 2f
        val halfH = k.h / 2f
        val saved = c.save()
        c.translate(k.cx, k.cy)
        if (down) {
            c.translate(0f, r * 0.10f)
            c.scale(PRESSED_SCALE, PRESSED_SCALE)
        }
        shape(c, halfW, halfH, if (down) r * 0.05f else r * 0.17f, -r * 0.07f, shadowSoftPaint)
        shape(c, halfW, halfH, if (down) r * 0.03f else r * 0.08f, -r * 0.01f, shadowHardPaint)
        shape(c, halfW, halfH, 0f, 0f, rimPaint)
        shape(c, halfW, halfH, 0f, r * 0.13f, groovePaint)
        val cap = when {
            k.spec.kind == KeyKind.DELETE -> redCapPaint
            halfW > halfH + 0.5f -> pillCapPaint
            else -> roundCapPaint
        }
        shape(c, halfW, halfH, 0f, r * 0.17f, cap)
        rect.set(-halfW + r * 0.45f, -halfH + r * 0.26f, halfW - r * 0.45f, -halfH + r * 0.62f)
        c.drawOval(rect, glossPaint)
        drawLegend(c, k.spec, shift)
        c.restoreToCount(saved)
    }

    /** The character a pressed key "types" onto the paper strip, above the finger. */
    fun drawStamp(c: Canvas, g: KeyboardGeometry, k: KeyboardGeometry.PlacedKey, text: String) {
        val margin = g.unit * 0.4f
        c.drawText(text, k.cx.coerceIn(margin, g.width - margin), stampBaseline, stampPaint)
    }

    private fun drawLegend(c: Canvas, spec: KeySpec, shift: ShiftMode) {
        when (spec.kind) {
            KeyKind.LETTER, KeyKind.SYMBOL -> c.drawText(spec.label, 0f, legendBaseline, legendPaint)
            KeyKind.SHIFT -> when (shift) {
                ShiftMode.OFF -> c.drawPath(shiftArrow, iconStrokePaint)
                ShiftMode.ONCE -> c.drawPath(shiftArrow, iconFillPaint)
                ShiftMode.LOCKED -> {
                    c.drawPath(shiftArrow, iconFillPaint)
                    c.drawRect(shiftLockBar, iconFillPaint)
                }
            }
            KeyKind.DELETE -> {
                c.drawPath(deleteTag, iconStrokePaint)
                c.drawPath(deleteCross, iconStrokePaint)
            }
            KeyKind.ENTER -> c.drawPath(enterArrow, iconStrokePaint)
            KeyKind.SPACE -> Unit
        }
    }

    /** A round key or a pill centred on (0, 0), moved down by [dy] and shrunk by [inset]. */
    private fun shape(c: Canvas, halfW: Float, halfH: Float, dy: Float, inset: Float, paint: Paint) {
        rect.set(-halfW + inset, -halfH + inset + dy, halfW - inset, halfH - inset + dy)
        val corner = halfH - inset
        c.drawRoundRect(rect, corner, corner, paint)
    }

    /** Icons for shift, delete and enter, sized to a cap of height [s]. */
    private fun buildIcons(s: Float) {
        shiftArrow.reset()
        shiftArrow.moveTo(0f, -0.30f * s)
        shiftArrow.lineTo(0.27f * s, -0.02f * s)
        shiftArrow.lineTo(0.13f * s, -0.02f * s)
        shiftArrow.lineTo(0.13f * s, 0.22f * s)
        shiftArrow.lineTo(-0.13f * s, 0.22f * s)
        shiftArrow.lineTo(-0.13f * s, -0.02f * s)
        shiftArrow.lineTo(-0.27f * s, -0.02f * s)
        shiftArrow.close()
        shiftLockBar.set(-0.13f * s, 0.28f * s, 0.13f * s, 0.33f * s)

        deleteTag.reset()
        deleteTag.moveTo(-0.36f * s, 0f)
        deleteTag.lineTo(-0.16f * s, -0.20f * s)
        deleteTag.lineTo(0.34f * s, -0.20f * s)
        deleteTag.lineTo(0.34f * s, 0.20f * s)
        deleteTag.lineTo(-0.16f * s, 0.20f * s)
        deleteTag.close()
        deleteCross.reset()
        deleteCross.moveTo(0f, -0.08f * s)
        deleteCross.lineTo(0.16f * s, 0.08f * s)
        deleteCross.moveTo(0.16f * s, -0.08f * s)
        deleteCross.lineTo(0f, 0.08f * s)

        enterArrow.reset()
        enterArrow.moveTo(0.30f * s, -0.20f * s)
        enterArrow.lineTo(0.30f * s, 0.06f * s)
        enterArrow.lineTo(-0.26f * s, 0.06f * s)
        enterArrow.moveTo(-0.13f * s, -0.07f * s)
        enterArrow.lineTo(-0.26f * s, 0.06f * s)
        enterArrow.lineTo(-0.13f * s, 0.19f * s)
    }

    private companion object {
        const val TYPEBAR_ROWS = 3 // the three letter rows hang from typebars; the space row does not
        const val PRESSED_SCALE = 0.93f

        fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        fun stroke(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }
}
