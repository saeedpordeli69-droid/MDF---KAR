package ir.mdfkar.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.max

/** Live schematic preview of the selected furniture layout. This is a design aid, not a CAD drawing. */
internal class ProductPreviewView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var productType = 5
    private var widthMm = 1800.0
    private var heightMm = 1100.0
    private var depthMm = 700.0
    private var shelves = 3
    private var doors = 2
    private var leftModule = 0 // drawers, cabinet, open shelves, open space
    private var rightModule = 1
    private var drawerCount = 2
    private var bookcaseBase = 0 // cabinet, drawers, open shelves
    private var bookcaseBaseHeight = 600.0
    private var english = false

    fun configure(
        productType: Int, widthMm: Double, heightMm: Double, depthMm: Double,
        shelves: Int, doors: Int, leftModule: Int, rightModule: Int,
        drawerCount: Int, bookcaseBase: Int, bookcaseBaseHeight: Double, english: Boolean
    ) {
        this.productType = productType
        this.widthMm = widthMm.coerceAtLeast(1.0)
        this.heightMm = heightMm.coerceAtLeast(1.0)
        this.depthMm = depthMm.coerceAtLeast(1.0)
        this.shelves = shelves.coerceIn(0, 30)
        this.doors = doors.coerceIn(0, 12)
        this.leftModule = leftModule.coerceIn(0, 3)
        this.rightModule = rightModule.coerceIn(0, 3)
        this.drawerCount = drawerCount.coerceIn(1, 8)
        this.bookcaseBase = bookcaseBase.coerceIn(0, 2)
        this.bookcaseBaseHeight = bookcaseBaseHeight.coerceIn(1.0, this.heightMm)
        this.english = english
        invalidate()
    }

    private fun color(value: Int) = value
    private fun line(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, stroke: Float = 1.5f) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.color = color(0xFFB8D6E9.toInt())
        canvas.drawLine(x1, y1, x2, y2, paint)
    }
    private fun fill(canvas: Canvas, rect: RectF, c: Int) {
        paint.style = Paint.Style.FILL
        paint.color = c
        canvas.drawRoundRect(rect, 2f, 2f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.4f
        paint.color = color(0xFFD5E8F5.toInt())
        canvas.drawRoundRect(rect, 2f, 2f, paint)
    }
    private fun label(canvas: Canvas, value: String, x: Float, y: Float, size: Float, c: Int = 0xFFEAF5FF.toInt()) {
        paint.style = Paint.Style.FILL
        paint.color = c
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(value, x, y, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawColor(0xFF071B31.toInt())
        label(canvas, "LIVE DESIGN PREVIEW", w / 2f, 23f, 14f, 0xFF59B9FF.toInt())
        val maxW = w * 0.72f
        val maxH = h * 0.62f
        val ratio = minOf(maxW / widthMm.toFloat(), maxH / heightMm.toFloat()).coerceAtLeast(0.01f)
        val fw = widthMm.toFloat() * ratio
        val fh = heightMm.toFloat() * ratio
        val left = (w - fw) / 2f - 8f
        val top = 42f + (maxH - fh) / 2f
        val right = left + fw
        val bottom = top + fh
        val depth = (depthMm.toFloat() * ratio * 0.24f).coerceIn(8f, 28f)

        // Side and top faces provide a simple pseudo-3D perspective.
        val side = Path().apply { moveTo(right, top); lineTo(right + depth, top - depth * 0.48f); lineTo(right + depth, bottom - depth * 0.48f); lineTo(right, bottom); close() }
        paint.style = Paint.Style.FILL; paint.color = 0xFF426A83.toInt(); canvas.drawPath(side, paint)
        val topFace = Path().apply { moveTo(left, top); lineTo(left + depth, top - depth * 0.48f); lineTo(right + depth, top - depth * 0.48f); lineTo(right, top); close() }
        paint.color = 0xFF9BB7C8.toInt(); canvas.drawPath(topFace, paint)
        val body = RectF(left, top, right, bottom)
        fill(canvas, body, 0xFFB8C7C9.toInt())

        when (productType) {
            2 -> drawBookcase(canvas, left, top, right, bottom, fw, fh)
            3 -> drawDesk(canvas, left, top, right, bottom, fw, fh)
            4, 5 -> drawCounter(canvas, left, top, right, bottom, fw, fh)
            6 -> drawDecor(canvas, left, top, right, bottom, fw, fh)
            else -> drawCabinetLike(canvas, left, top, right, bottom, fw, fh)
        }
        line(canvas, left, bottom + 9f, right, bottom + 9f, 1f)
        label(canvas, "${widthMm.toInt()} × ${heightMm.toInt()} × ${depthMm.toInt()} mm", w / 2f, h - 10f, 11f, 0xFFB8D6E9.toInt())
    }

    private fun drawCounter(c: Canvas, l: Float, t: Float, r: Float, b: Float, fw: Float, fh: Float) {
        val mid = l + fw * 0.5f
        line(c, mid, t + 3f, mid, b - 2f, 2f)
        drawModule(c, l + 3f, t + 3f, mid - 2f, b - 3f, leftModule)
        drawModule(c, mid + 2f, t + 3f, r - 3f, b - 3f, rightModule)
        // Highlight the worktop.
        line(c, l, t + 2f, r, t + 2f, 3f)
    }

    private fun drawModule(c: Canvas, l: Float, t: Float, r: Float, b: Float, mode: Int) {
        val ww = r - l; val hh = b - t
        when (mode) {
            0 -> {
                val each = hh / drawerCount
                for (i in 0 until drawerCount) {
                    val y1 = t + i * each; val y2 = y1 + each - 1.5f
                    fill(c, RectF(l + 2f, y1 + 1f, r - 2f, y2), 0xFF9AAFB8.toInt())
                    line(c, l + ww * 0.42f, y1 + each * 0.5f, l + ww * 0.58f, y1 + each * 0.5f, 2f)
                }
            }
            1 -> {
                fill(c, RectF(l + 2f, t + 2f, r - 2f, b - 2f), 0xFF9FB4BD.toInt())
                line(c, (l + r) / 2f, t + 3f, (l + r) / 2f, b - 3f)
                line(c, (l + r) / 2f - 4f, (t + b) / 2f, (l + r) / 2f - 2f, (t + b) / 2f, 2f)
                line(c, (l + r) / 2f + 2f, (t + b) / 2f, (l + r) / 2f + 4f, (t + b) / 2f, 2f)
            }
            2 -> {
                for (i in 1..minOf(3, shelves)) {
                    val y = t + hh * i / (minOf(3, shelves) + 1f)
                    line(c, l + 2f, y, r - 2f, y, 2f)
                }
            }
            else -> {
                paint.style = Paint.Style.STROKE; paint.color = 0xFF7B9BAE.toInt(); paint.strokeWidth = 1f
                c.drawRect(l + 3f, t + 3f, r - 3f, b - 3f, paint)
            }
        }
    }

    private fun drawBookcase(c: Canvas, l: Float, t: Float, r: Float, b: Float, fw: Float, fh: Float) {
        val baseH = (fh * (bookcaseBaseHeight / heightMm).toFloat()).coerceIn(fh * 0.18f, fh * 0.72f)
        val baseTop = b - baseH
        line(c, l + 2f, baseTop, r - 2f, baseTop, 2.5f)
        for (i in 1..shelves.coerceAtMost(8)) {
            val y = t + (baseTop - t) * i / (shelves.coerceAtMost(8) + 1f)
            line(c, l + 2f, y, r - 2f, y, 2f)
        }
        // Match the vertical divider that is included in the generated bookcase cut list.
        line(c, l + fw * 0.5f, t + 2f, l + fw * 0.5f, baseTop - 2f, 2f)
        when (bookcaseBase) {
            0 -> drawModule(c, l + 3f, baseTop + 2f, r - 3f, b - 2f, 1)
            1 -> drawModule(c, l + 3f, baseTop + 2f, r - 3f, b - 2f, 0)
            else -> {
                val hh = b - baseTop
                line(c, l + 2f, baseTop + hh / 2f, r - 2f, baseTop + hh / 2f, 2f)
            }
        }
    }

    private fun drawDesk(c: Canvas, l: Float, t: Float, r: Float, b: Float, fw: Float, fh: Float) {
        val topH = max(5f, fh * 0.08f)
        fill(c, RectF(l, t, r, t + topH), 0xFF839DA9.toInt())
        val legW = max(5f, fw * 0.09f)
        fill(c, RectF(l + fw * 0.08f, t + topH, l + fw * 0.08f + legW, b), 0xFF8EA5AE.toInt())
        fill(c, RectF(r - fw * 0.08f - legW, t + topH, r - fw * 0.08f, b), 0xFF8EA5AE.toInt())
        line(c, l + legW, t + fh * 0.68f, r - legW, t + fh * 0.68f, 3f)
    }

    private fun drawDecor(c: Canvas, l: Float, t: Float, r: Float, b: Float, fw: Float, fh: Float) {
        for (i in 1..3) {
            val x = l + fw * i / 4f
            line(c, x, t + 3f, x, b - 3f, 2f)
        }
        for (i in 1..shelves.coerceAtMost(4)) {
            val y = t + fh * i / (shelves.coerceAtMost(4) + 1f)
            line(c, l + 2f, y, r - 2f, y, 3f)
        }
    }

    private fun drawCabinetLike(c: Canvas, l: Float, t: Float, r: Float, b: Float, fw: Float, fh: Float) {
        val baseType = if (productType == 1 || productType == 7 || productType == 8) 1 else 2
        if (productType == 1 || productType == 7 || productType == 8) {
            if (doors > 0) {
                val each = fw / doors
                for (i in 0 until doors) {
                    val x1 = l + i * each
                    fill(c, RectF(x1 + 2f, t + 3f, x1 + each - 2f, b - 3f), 0xFF9FB4BD.toInt())
                    line(c, x1 + each * 0.82f, t + fh * 0.5f, x1 + each * 0.82f, t + fh * 0.5f + 2f, 2f)
                }
            } else {
                for (i in 1..shelves.coerceAtMost(8)) line(c, l + 2f, t + fh * i / (shelves.coerceAtMost(8) + 1f), r - 2f, t + fh * i / (shelves.coerceAtMost(8) + 1f), 2f)
            }
        } else {
            for (i in 1..shelves.coerceAtMost(8)) line(c, l + 2f, t + fh * i / (shelves.coerceAtMost(8) + 1f), r - 2f, t + fh * i / (shelves.coerceAtMost(8) + 1f), 2f)
        }
    }
}
