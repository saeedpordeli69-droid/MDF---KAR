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
    private val dimensionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppTheme.TEXT
        textSize = dp(8).toFloat()
        textAlign = Paint.Align.CENTER
    }
    private val usageTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppTheme.TEXT
        textSize = dp(11).toFloat()
        textAlign = Paint.Align.RIGHT
    }
    private val sheetInfoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppTheme.TEXT
        textSize = dp(9).toFloat()
        textAlign = Paint.Align.LEFT
    }
    private val usageTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppTheme.FIELD_BORDER
        style = Paint.Style.FILL
    }
    private val usageFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppTheme.ACCENT
        style = Paint.Style.FILL
    }
    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    private fun dimensionLabel(value: Double): String {
        val raw = value.toInt().toString()
        return buildString(raw.length) {
            raw.forEach { ch -> append(if (ch in '0'..'9') persianDigits[ch - '0'] else ch) }
        }
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
        val headerHeight = dp(44).toFloat()
        val availableH = height - pad * 2 - headerHeight
        if (availableW <= 0 || availableH <= 0 || sw <= 0f || sh <= 0f) return

        // Show how much of the selected sheet is occupied by parts.
        val sheetParts = currentPlacements.filter { it.sheet == currentSheet }
        val sheetArea = sw.toDouble() * sh.toDouble()
        val usedArea = sheetParts.sumOf { it.width * it.height }
        val usage = if (sheetArea > 0.0) (usedArea / sheetArea).coerceIn(0.0, 1.0) else 0.0
        val percent = (usage * 100.0).toInt()
        usageTextPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            translate("استفاده: ${dimensionLabel(percent.toDouble())}٪", "Used: $percent%"),
            width - pad,
            pad + usageTextPaint.textSize,
            usageTextPaint
        )
        val emptyPercent = 100 - percent
        val sheetInfo = translate(
            "ابعاد: ${dimensionLabel(sw.toDouble())} × ${dimensionLabel(sh.toDouble())} | فضای خالی: ${dimensionLabel(emptyPercent.toDouble())}٪",
            "Size: ${sw.toInt()} × ${sh.toInt()} mm | Free: $emptyPercent%"
        )
        sheetInfoPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            sheetInfo,
            pad,
            pad + dp(27).toFloat(),
            sheetInfoPaint
        )
        val barLeft = pad
        val barRight = width - pad
        val barTop = pad + dp(31).toFloat()
        val barBottom = barTop + dp(5).toFloat()
        val barRadius = dp(3).toFloat()
        canvas.drawRoundRect(barLeft, barTop, barRight, barBottom, barRadius, barRadius, usageTrackPaint)
        if (usage > 0.0) {
            val filledRight = barLeft + (barRight - barLeft) * usage.toFloat()
            canvas.drawRoundRect(barLeft, barTop, filledRight, barBottom, barRadius, barRadius, usageFillPaint)
        }

        val scale = min(availableW / sw, availableH / sh)
        val sheetPixelW = sw * scale
        val sheetPixelH = sh * scale
        val left = (width - sheetPixelW) / 2f
        val top = headerHeight + pad + (availableH - sheetPixelH) / 2f
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
            val partW = r - l
            val partH = b - t
            if (partW > dp(28) && partH > dp(16)) {
                val centerX = (l + r) / 2f
                val centerY = (t + b) / 2f
                if (partW > dp(58) && partH > dp(32)) {
                    val name = part.name.take(14)
                    val dimensions = "${dimensionLabel(part.width)} × ${dimensionLabel(part.height)}"
                    val metrics = textPaint.fontMetrics
                    val lineGap = dp(2).toFloat()
                    val totalTextHeight = (metrics.descent - metrics.ascent) + dimensionPaint.textSize + lineGap
                    val firstBaseline = centerY - totalTextHeight / 2f - metrics.ascent
                    canvas.drawText(name, centerX, firstBaseline, textPaint)
                    canvas.drawText(dimensions, centerX, firstBaseline + metrics.descent + lineGap + dimensionPaint.textSize, dimensionPaint)
                    if (part.rotated && partW > dp(78)) {
                        canvas.drawText("↻", r - dp(9), t + dp(12).toFloat(), dimensionPaint)
                    }
                } else {
                    canvas.drawText(part.name.take(10), centerX, centerY, textPaint)
                }
            }
        }
        if (currentPlacements.isEmpty()) {
            canvas.drawText(translate("پس از محاسبه، نقشه اینجا نمایش داده می‌شود", "Layout appears here after calculation"), width / 2f, height / 2f, textPaint)
        } else if (currentPlacements.none { it.sheet == currentSheet }) {
            canvas.drawText(translate("این ورق قطعه‌ای ندارد", "No pieces on this sheet"), width / 2f, height / 2f, textPaint)
        }
    }
}
