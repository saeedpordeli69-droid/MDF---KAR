package ir.mdfkar.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/** Draws the sheet cutting plan. Kept separate from MainActivity to keep screen logic smaller. */
internal class SheetLayoutView(
    context: Context,
    private val placements: () -> List<SheetPlacement>,
    private val sheetWidth: () -> Double,
    private val sheetHeight: () -> Double,
    private val selectedSheet: () -> Int,
    private val sheetCount: () -> Int,
    private val onSelectedSheetChanged: (Int) -> Unit,
    private val partColor: (String) -> Int,
    private val translate: (String, String) -> String,
    private val dp: (Int) -> Int
) : View(context) {
    private var downX = 0f
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppTheme.ACCENT
        style = Paint.Style.STROKE
        strokeWidth = dp(1).toFloat()
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppTheme.TEXT
        textSize = dp(10).toFloat()
        textAlign = Paint.Align.CENTER
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { downX = event.x; return true }
            MotionEvent.ACTION_UP -> {
                val delta = event.x - downX
                val total = sheetCount()
                var selected = selectedSheet()
                if (kotlin.math.abs(delta) > dp(45) && total > 1) {
                    if (delta < 0 && selected < total - 1) selected++
                    else if (delta > 0 && selected > 0) selected--
                    onSelectedSheetChanged(selected)
                    invalidate()
                }
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(AppTheme.FIELD)
        val currentPlacements = placements()
        val currentSheet = selectedSheet()
        val sw = sheetWidth().toFloat()
        val sh = sheetHeight().toFloat()
        val pad = dp(12).toFloat()
        val availableW = width - pad * 2
        val availableH = height - pad * 2
        if (availableW <= 0 || availableH <= 0 || sw <= 0f || sh <= 0f) return
        val scale = min(availableW / sw, availableH / sh)
        val sheetPixelW = sw * scale
        val sheetPixelH = sh * scale
        val left = (width - sheetPixelW) / 2f
        val top = (height - sheetPixelH) / 2f
        canvas.drawRect(left, top, left + sheetPixelW, top + sheetPixelH, border)
        currentPlacements.forEach { part ->
            if (part.sheet != currentSheet) return@forEach
            val l = left + part.x.toFloat() * scale
            val t = top + part.y.toFloat() * scale
            val r = l + part.width.toFloat() * scale
            val b = t + part.height.toFloat() * scale
            fill.color = partColor(part.name)
            canvas.drawRect(l, t, r, b, fill)
            canvas.drawRect(l, t, r, b, border)
            if (r - l > dp(24) && b - t > dp(15)) {
                canvas.drawText(part.name.take(14), (l + r) / 2f, (t + b) / 2f, textPaint)
            }
        }
        if (currentPlacements.isEmpty()) {
            canvas.drawText(translate("پس از محاسبه، نقشه اینجا نمایش داده می‌شود", "Layout appears here after calculation"), width / 2f, height / 2f, textPaint)
        } else if (currentPlacements.none { it.sheet == currentSheet }) {
            canvas.drawText(translate("این ورق قطعه‌ای ندارد", "No pieces on this sheet"), width / 2f, height / 2f, textPaint)
        }
    }
}
