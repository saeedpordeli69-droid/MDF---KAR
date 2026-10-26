package ir.mdfkar.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {

    private val dark = Color.rgb(34, 43, 48)
    private val green = Color.rgb(36, 130, 93)
    private val pale = Color.rgb(244, 247, 245)
    private val ink = Color.rgb(35, 43, 40)

    private var english = false
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var resultText: TextView
    private lateinit var layoutView: SheetLayoutView

    private lateinit var widthInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var depthInput: EditText
    private lateinit var thicknessInput: EditText
    private lateinit var shelfInput: EditText
    private lateinit var doorInput: EditText
    private lateinit var plinthInput: EditText
    private lateinit var backThicknessInput: EditText
    private lateinit var sheetWidthInput: EditText
    private lateinit var sheetHeightInput: EditText
    private lateinit var kerfInput: EditText
    private lateinit var cabinetSpinner: Spinner

    private lateinit var includeBackCheck: CheckBox
    private lateinit var legsCheck: CheckBox
    private lateinit var doorPvcCheck: CheckBox
    private lateinit var sidePvcCheck: CheckBox
    private lateinit var shelfPvcCheck: CheckBox

    private var latestPlacements: List<Placement> = emptyList()
    private var latestSheetW = 2800.0
    private var latestSheetH = 2070.0

    data class Part(val name: String, val width: Double, val height: Double, val quantity: Int = 1, val rotate: Boolean = true)
    data class Placement(val name: String, val x: Double, val y: Double, val width: Double, val height: Double, val rotated: Boolean, val sheet: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadSettings()
        buildScreen()
    }

    private fun prefs() = getSharedPreferences("mdf_kar_settings", MODE_PRIVATE)

    private fun loadSettings() {
        val p = prefs()
        english = p.getBoolean("english", false)
    }

    private fun saveSettings() {
        prefs().edit()
            .putBoolean("english", english)
            .putString("width", widthInput.text.toString())
            .putString("height", heightInput.text.toString())
            .putString("depth", depthInput.text.toString())
            .putString("thickness", thicknessInput.text.toString())
            .putString("shelves", shelfInput.text.toString())
            .putString("doors", doorInput.text.toString())
            .putString("plinth", plinthInput.text.toString())
            .putString("backThickness", backThicknessInput.text.toString())
            .putString("sheetWidth", sheetWidthInput.text.toString())
            .putString("sheetHeight", sheetHeightInput.text.toString())
            .putString("kerf", kerfInput.text.toString())
            .putInt("cabinetType", cabinetSpinner.selectedItemPosition)
            .putBoolean("includeBack", includeBackCheck.isChecked)
            .putBoolean("legs", legsCheck.isChecked)
            .putBoolean("doorPvc", doorPvcCheck.isChecked)
            .putBoolean("sidePvc", sidePvcCheck.isChecked)
            .putBoolean("shelfPvc", shelfPvcCheck.isChecked)
            .apply()
    }

    private fun saved(key: String, fallback: String) = prefs().getString(key, fallback) ?: fallback

    private fun field(hintFa: String, hintEn: String, key: String, fallback: String): EditText {
        return EditText(this).apply {
            hint = tr(hintFa, hintEn)
            setText(saved(key, fallback))
            textSize = 16f
            setSingleLine(true)
            setTextColor(ink)
            setHintTextColor(Color.GRAY)
            setPadding(16, 8, 16, 8)
            layoutDirection = View.LAYOUT_DIRECTION.LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setBackgroundColor(Color.WHITE)
        }
    }

    private fun tr(fa: String, en: String) = if (english) en else fa

    private fun label(fa: String, en: String, size: Float = 15f): TextView {
        return TextView(this).apply {
            text = tr(fa, en)
            textSize = size
            setTextColor(ink)
            if (size >= 18f) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 8)
        }
    }

    private fun button(fa: String, en: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = tr(fa, en)
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener { action() }
        }
    }

    private fun check(fa: String, en: String, key: String, default: Boolean): CheckBox {
        return CheckBox(this).apply {
            text = tr(fa, en)
            textSize = 14f
            setTextColor(ink)
            isChecked = prefs().getBoolean(key, default)
        }
    }

    private fun addField(view: View) {
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = 7
        content.addView(view, lp)
    }

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pale)
            layoutDirection = if (english) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
            setBackgroundColor(dark)
        }
        val title = TextView(this).apply {
            text = tr("ام‌دی‌اف‌کار | محاسبه و برش", "MDF Kar | Cut Planner")
            textSize = 21f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val languageButton = Button(this).apply {
            text = if (english) "فارسی" else "English"
            isAllCaps = false
            setOnClickListener {
                saveSettings()
                english = !english
                prefs().edit().putBoolean("english", english).apply()
                buildScreen()
            }
        }
        header.addView(title)
        header.addView(languageButton)
        root.addView(header)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 22)
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(label("ابعاد کابینت (میلی‌متر)", "Cabinet dimensions (mm)", 18f))
        cabinetSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                if (english) arrayOf("Base cabinet", "Wall cabinet") else arrayOf("کابینت زمینی", "کابینت دیواری")
            )
            setSelection(prefs().getInt("cabinetType", 0).coerceIn(0, 1))
        }
        addField(cabinetSpinner)
        widthInput = field("عرض کابینت", "Cabinet width", "width", "800")
        heightInput = field("ارتفاع کابینت", "Cabinet height", "height", "720")
        depthInput = field("عمق کابینت", "Cabinet depth", "depth", "560")
        thicknessInput = field("ضخامت ورق بدنه", "Carcass board thickness", "thickness", "16")
        shelfInput = field("تعداد طبقه داخلی", "Number of shelves", "shelves", "1")
        doorInput = field("تعداد در (۰ تا ۲)", "Number of doors (0–2)", "doors", "2")
        plinthInput = field("ارتفاع پایه/پاخور", "Plinth / leg height", "plinth", "100")
        backThicknessInput = field("ضخامت پشت‌بند", "Back panel thickness", "backThickness", "3")
        listOf(widthInput, heightInput, depthInput, thicknessInput, shelfInput, doorInput, plinthInput, backThicknessInput).forEach { addField(it) }

        content.addView(label("تنظیمات ورق و تیغه", "Sheet and saw settings", 18f))
        sheetWidthInput = field("عرض ورق (پیش‌فرض ۲۰۷۰)", "Sheet width (default 2070)", "sheetWidth", "2070")
        sheetHeightInput = field("طول ورق (پیش‌فرض ۲۸۰۰)", "Sheet length (default 2800)", "sheetHeight", "2800")
        kerfInput = field("عرض تیغه (پیش‌فرض ۳)", "Saw kerf (default 3)", "kerf", "3")
        listOf(sheetWidthInput, sheetHeightInput, kerfInput).forEach { addField(it) }

        content.addView(label("گزینه‌های محاسبه", "Calculation options", 18f))
        includeBackCheck = check("محاسبه پشت‌بند", "Include back panel", "includeBack", true)
        legsCheck = check("کسر ارتفاع پاخور از بدنه کابینت زمینی", "Subtract plinth from base cabinet body", "legs", true)
        doorPvcCheck = check("PVC چهار لبه درها", "PVC on all door edges", "doorPvc", true)
        sidePvcCheck = check("PVC لبه جلویی دو پهلو", "PVC on front edges of both sides", "sidePvc", true)
        shelfPvcCheck = check("PVC لبه جلویی کف و طبقه‌ها", "PVC on front edges of bottom and shelves", "shelfPvc", true)
        listOf(includeBackCheck, legsCheck, doorPvcCheck, sidePvcCheck, shelfPvcCheck).forEach { content.addView(it) }

        content.addView(button("محاسبه قطعات و چیدمان برش", "Calculate parts and cutting layout") {
            saveSettings()
            calculate()
        })
        content.addView(label("نقشه چیدمان ورق", "Sheet layout", 18f))
        layoutView = SheetLayoutView()
        layoutView.setBackgroundColor(Color.WHITE)
        content.addView(layoutView, LinearLayout.LayoutParams(-1, dp(240)))
        resultText = label("ابعاد را وارد کنید و محاسبه را بزنید.", "Enter dimensions and calculate.")
        resultText.textSize = 14f
        resultText.setPadding(0, 12, 0, 12)
        content.addView(resultText)
        content.addView(label("توجه: این چیدمان تخمینی است؛ پیش از برش واقعی، ابعاد، جهت رگه، لبه‌چسبانی و تلرانس دستگاه را بررسی کنید.", "Warning: this is an estimated layout. Verify dimensions, grain direction, edge banding and machine tolerances before cutting.", 12f))
        setContentView(root)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun number(raw: String): Double? {
        val normalized = raw
            .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3')
            .replace('۴', '4').replace('۵', '5').replace('۶', '6').replace('۷', '7')
            .replace('۸', '8').replace('۹', '9')
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3')
            .replace('٤', '4').replace('٥', '5').replace('٦', '6').replace('٧', '7')
            .replace('٨', '8').replace('٩', '9')
            .replace(',', '.').trim()
        return normalized.toDoubleOrNull()
    }

    private fun fmt(v: Double): String = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US)).format(v)

    private fun faDigits(s: String): String {
        val map = charArrayOf('۰','۱','۲','۳','۴','۵','۶','۷','۸','۹')
        return s.map { if (it in '0'..'9') map[it - '0'] else it }.joinToString("")
    }

    private fun calculate() {
        val w = number(widthInput.text.toString())
        val h = number(heightInput.text.toString())
        val d = number(depthInput.text.toString())
        val t = number(thicknessInput.text.toString())
        val shelves = number(shelfInput.text.toString())?.toInt()
        val doors = number(doorInput.text.toString())?.toInt()
        val plinthInputValue = number(plinthInput.text.toString())
        val backT = number(backThicknessInput.text.toString())
        val sw = number(sheetWidthInput.text.toString())
        val sh = number(sheetHeightInput.text.toString())
        val kerf = number(kerfInput.text.toString())

        if (w == null || h == null || d == null || t == null || shelves == null ||
            doors == null || plinthInputValue == null || backT == null || sw == null ||
            sh == null || kerf == null || w <= 0 || h <= 0 || d <= 0 || t <= 0 ||
            shelves !in 0..30 || doors !in 0..2 || plinthInputValue < 0 || backT < 0 ||
            sw <= 0 || sh <= 0 || kerf < 0) {
            resultText.text = tr(
                "لطفاً ابعاد مثبت وارد کنید. تعداد طبقه باید ۰ تا ۳۰ و تعداد در ۰ تا ۲ باشد.",
                "Enter valid positive dimensions. Shelves must be 0–30 and doors 0–2."
            )
            return
        }

        val isBase = cabinetSpinner.selectedItemPosition == 0
        val plinth = if (isBase && legsCheck.isChecked) plinthInputValue else 0.0
        val bodyH = h - plinth
        val innerW = w - 2 * t
        val innerD = d
        if (bodyH <= 0 || innerW <= 0 || innerD <= 0) {
            resultText.text = tr("ابعاد پس از کسر ضخامت/پاخور معتبر نیست.", "Dimensions are invalid after subtracting board thickness/plinth.")
            return
        }

        val parts = mutableListOf<Part>()
        parts += Part(tr("پهلو", "Side"), d, bodyH, 2)
        parts += Part(tr("کف", "Bottom"), innerW, d)
        if (shelves > 0) parts += Part(tr("طبقه", "Shelf"), innerW, d, shelves)
        if (isBase) {
            parts += Part(tr("ریل بالا", "Top rail"), max(100.0, innerW / 8), t, 2)
        } else {
            parts += Part(tr("بالا", "Top"), innerW, d)
        }

        if (includeBackCheck.isChecked) {
            parts += Part(tr("پشت‌بند", "Back panel"), w, bodyH, 1, false)
        }
        if (doors > 0) {
            val doorW = (w - 3.0) / doors
            val doorH = bodyH - 3.0
            if (doorW > 0 && doorH > 0) parts += Part(tr("درب", "Door"), doorW, doorH, doors)
        }

        val boardParts = parts.filter { it.name != tr("پشت‌بند", "Back panel") }
        val layout = packParts(boardParts, sw, sh, kerf)
        latestPlacements = layout.placements
        latestSheetW = sw
        latestSheetH = sh
        layoutView.invalidate()

        val boardArea = boardParts.sumOf { it.width * it.height * it.quantity }
        val sheetArea = sw * sh
        val sheetCount = layout.sheetCount
        val unusedArea = max(0.0, sheetCount * sheetArea - boardArea)
        val wastePercent = if (sheetCount > 0) unusedArea / (sheetCount * sheetArea) * 100.0 else 0.0
        val backArea = if (includeBackCheck.isChecked) w * bodyH / 1_000_000.0 else 0.0

        var pvcMeters = 0.0
        if (doorPvcCheck.isChecked && doors > 0) {
            val doorW = (w - 3.0) / doors
            val doorH = bodyH - 3.0
            pvcMeters += doors * 2 * (doorW + doorH) / 1000.0
        }
        if (sidePvcCheck.isChecked) pvcMeters += 2 * bodyH / 1000.0
        if (shelfPvcCheck.isChecked) pvcMeters += (1 + shelves) * innerW / 1000.0

        val listText = parts.joinToString("\n") {
            "• ${it.name}: ${fmt(it.width)} × ${fmt(it.height)} mm × ${it.quantity}"
        }
        val summaryFa = """
            فهرست قطعات:
            $listText

            ورق بدنه: ${sheetCount} عدد (${fmt(sw)} × ${fmt(sh)} میلی‌متر)
            مساحت قطعات بدنه: ${fmt(boardArea / 1_000_000.0)} مترمربع
            دورریز تخمینی بر اساس مساحت: ${fmt(wastePercent)}٪
            پشت‌بند: ${if (includeBackCheck.isChecked) "${fmt(backArea)} مترمربع (جداگانه)" else "محاسبه نشده"}
            PVC تخمینی: ${fmt(pvcMeters)} متر
            عرض تیغه لحاظ‌شده در چیدمان: ${fmt(kerf)} میلی‌متر
            تعداد قطعات روی نقشه: ${layout.placements.size}

            هشدار: این نقشه از چیدمان ردیفی/قفسه‌ای استفاده می‌کند و تضمین بهینه‌ترین برش صنعتی نیست. پشت‌بند جدا محاسبه شده و در نقشه ورق بدنه نیست.
        """.trimIndent()
        val summaryEn = """
            Parts list:
            $listText

            Carcass sheets: $sheetCount (${fmt(sw)} × ${fmt(sh)} mm)
            Carcass part area: ${fmt(boardArea / 1_000_000.0)} m²
            Estimated area-based waste: ${fmt(wastePercent)}%
            Back panel: ${if (includeBackCheck.isChecked) "${fmt(backArea)} m² (separate)" else "not included"}
            Estimated PVC: ${fmt(pvcMeters)} m
            Saw kerf used in layout: ${fmt(kerf)} mm
            Pieces placed: ${layout.placements.size}

            Warning: this uses a shelf/row packing heuristic and is not guaranteed to be an industrial optimum. The back panel is estimated separately and is not placed on the carcass sheet layout.
        """.trimIndent()
        val output = if (english) summaryEn else faDigits(summaryFa)
        resultText.text = output
        saveSettings()
    }

    private data class PackResult(val placements: List<Placement>, val sheetCount: Int)

    /*
     * Shelf/row heuristic: sort largest pieces first, place each in an existing row
     * if possible (including rotation), otherwise create a new row or new sheet.
     * Kerf is added between adjacent pieces and rows. This is a planning estimate,
     * not a certified guillotine/CNC optimizer.
     */
    private fun packParts(parts: List<Part>, sheetW: Double, sheetH: Double, kerf: Double): PackResult {
        val expanded = mutableListOf<Part>()
        parts.forEach { p ->
            repeat(p.quantity) { expanded += p.copy(quantity = 1) }
        }
        val sorted = expanded.sortedByDescending { max(it.width, it.height) * min(it.width, it.height) }
        data class Row(var y: Double, var height: Double, var x: Double)
        val rowsBySheet = mutableListOf<MutableList<Row>>()
        val placements = mutableListOf<Placement>()

        for (part in sorted) {
            var placed = false
            for (sheetIndex in rowsBySheet.indices) {
                val rows = rowsBySheet[sheetIndex]
                for (row in rows) {
                    val remaining = sheetW - row.x
                    val options = mutableListOf<Triple<Double, Double, Boolean>>()
                    if (part.width <= remaining && part.height <= row.height) options += Triple(part.width, part.height, false)
                    if (part.rotate && part.height <= remaining && part.width <= row.height) options += Triple(part.height, part.width, true)
                    val choice = options.minByOrNull { it.first * it.second }
                    if (choice != null) {
                        placements += Placement(part.name, row.x, row.y, choice.first, choice.second, choice.third, sheetIndex)
                        row.x += choice.first + kerf
                        placed = true
                        break
                    }
                }
                if (placed) break

                val nextY = if (rows.isEmpty()) 0.0 else rows.maxOf { it.y + it.height } + kerf
                val orientations = mutableListOf<Triple<Double, Double, Boolean>>()
                if (part.width <= sheetW && part.height <= sheetH - nextY) orientations += Triple(part.width, part.height, false)
                if (part.rotate && part.height <= sheetW && part.width <= sheetH - nextY) orientations += Triple(part.height, part.width, true)
                val choice = orientations.minByOrNull { it.second }
                if (choice != null) {
                    rows += Row(nextY, choice.second, choice.first + kerf)
                    placements += Placement(part.name, 0.0, nextY, choice.first, choice.second, choice.third, sheetIndex)
                    placed = true
                    break
                }
            }

            if (!placed) {
                val orientations = mutableListOf<Triple<Double, Double, Boolean>>()
                if (part.width <= sheetW && part.height <= sheetH) orientations += Triple(part.width, part.height, false)
                if (part.rotate && part.height <= sheetW && part.width <= sheetH) orientations += Triple(part.height, part.width, true)
                val choice = orientations.minByOrNull { it.second }
                if (choice != null) {
                    val newIndex = rowsBySheet.size
                    val row = Row(0.0, choice.second, choice.first + kerf)
                    rowsBySheet += mutableListOf(row)
                    placements += Placement(part.name, 0.0, 0.0, choice.first, choice.second, choice.third, newIndex)
                } else {
                    // Keep the piece visible as an overflow placement; the output warns by dimensions.
                    val newIndex = rowsBySheet.size
                    rowsBySheet += mutableListOf(Row(0.0, min(part.height, sheetH), min(part.width, sheetW)))
                    placements += Placement(part.name, 0.0, 0.0, part.width, part.height, false, newIndex)
                }
            }
        }
        return PackResult(placements, rowsBySheet.size)
    }

    private inner class SheetLayoutView : View(this@MainActivity) {
        private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = dp(10).toFloat()
            textAlign = Paint.Align.CENTER
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.WHITE)
            val pad = dp(12).toFloat()
            val availableW = width - pad * 2
            val availableH = height - pad * 2
            if (availableW <= 0 || availableH <= 0) return
            val scale = min(availableW / latestSheetW.toFloat(), availableH / latestSheetH.toFloat())
            val sheetPixelW = latestSheetW.toFloat() * scale
            val sheetPixelH = latestSheetH.toFloat() * scale
            val left = (width - sheetPixelW) / 2f
            val top = (height - sheetPixelH) / 2f
            canvas.drawRect(left, top, left + sheetPixelW, top + sheetPixelH, border)
            val colors = intArrayOf(
                Color.rgb(188, 220, 202), Color.rgb(202, 216, 235),
                Color.rgb(245, 221, 173), Color.rgb(231, 199, 199),
                Color.rgb(214, 204, 235), Color.rgb(192, 224, 226)
            )
            latestPlacements.forEachIndexed { i, p ->
                if (p.sheet != 0) return@forEachIndexed
                val l = left + p.x.toFloat() * scale
                val t = top + p.y.toFloat() * scale
                val r = l + p.width.toFloat() * scale
                val b = t + p.height.toFloat() * scale
                fill.color = colors[i % colors.size]
                canvas.drawRect(l, t, r, b, fill)
                canvas.drawRect(l, t, r, b, border)
                if (r - l > dp(24) && b - t > dp(15)) {
                    val name = p.name.take(14)
                    canvas.drawText(name, (l + r) / 2f, (t + b) / 2f, textPaint)
                }
            }
            if (latestPlacements.isEmpty()) {
                canvas.drawText(
                    tr("پس از محاسبه، نقشه اینجا نمایش داده می‌شود", "Layout appears here after calculation"),
                    width / 2f, height / 2f, textPaint
                )
            }
        }
    }
}
