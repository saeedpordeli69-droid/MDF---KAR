package ir.mdfkar.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import java.io.File
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal data class CustomOrderPart(
    val name: String,
    val lengthMm: Double,
    val widthMm: Double,
    val quantity: Int,
    val thicknessMm: Double,
    val linkedCabinetId: String? = null,
    val referenceDimension: Int = -1
)

class MainActivity : Activity() {

    // Visual tokens live in AppTheme.kt so future appearance changes stay separate.
    internal val dark = AppTheme.BACKGROUND_TOP
    internal val green = AppTheme.PRIMARY
    internal val pale = AppTheme.BACKGROUND
    internal val ink = AppTheme.TEXT
    internal val card = AppTheme.CARD
    internal val fieldFill = AppTheme.FIELD
    internal val muted = AppTheme.MUTED

    internal var english = false
    internal lateinit var root: LinearLayout
    internal lateinit var content: LinearLayout
    internal lateinit var nav: LinearLayout
    internal lateinit var metricSheets: TextView
    internal lateinit var metricWaste: TextView
    internal lateinit var metricPieces: TextView
    internal lateinit var resultText: TextView
    internal lateinit var partsList: LinearLayout
    internal lateinit var layoutView: SheetLayoutView
    internal lateinit var sheetCounterLabel: TextView
    internal lateinit var previousSheetButton: Button
    internal lateinit var nextSheetButton: Button

    internal lateinit var widthInput: EditText
    internal lateinit var heightInput: EditText
    internal lateinit var depthInput: EditText
    internal lateinit var thicknessInput: EditText
    internal lateinit var shelfInput: EditText
    internal lateinit var doorInput: EditText
    internal lateinit var plinthInput: EditText
    internal lateinit var backThicknessInput: EditText
    internal lateinit var cabinetQuantityInput: EditText
    internal lateinit var sheetWidthInput: EditText
    internal lateinit var sheetHeightInput: EditText
    internal lateinit var kerfInput: EditText
    internal lateinit var cabinetSpinner: Spinner

    internal lateinit var includeBackCheck: CheckBox
    internal lateinit var legsCheck: CheckBox
    internal lateinit var doorPvcCheck: CheckBox
    internal lateinit var sidePvcCheck: CheckBox
    internal lateinit var shelfPvcCheck: CheckBox

    internal var latestPlacements: List<SheetPlacement> = emptyList()
    internal var latestSheetW = 2800.0
    internal var latestSheetH = 2070.0
    internal var latestSheetCount = 0
    internal var selectedSheetIndex = 0
    internal val orderCabinets = mutableListOf<CabinetSpec>()
    internal val customParts = mutableListOf<CustomOrderPart>()
    internal lateinit var orderListView: LinearLayout
    internal lateinit var customPartsListView: LinearLayout
    internal lateinit var customPartsSection: LinearLayout
    internal lateinit var customPartsToggleButton: TextView
    internal lateinit var customPartNameInput: EditText
    internal lateinit var customPartLengthInput: EditText
    internal lateinit var customPartWidthInput: EditText
    internal lateinit var customPartQtyInput: EditText
    internal lateinit var customPartThicknessInput: EditText
    internal lateinit var customPartModeSpinner: Spinner
    internal lateinit var customPartCabinetSpinner: Spinner
    internal lateinit var customPartDimensionSpinner: Spinner

    
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(3, 13, 27)
        window.navigationBarColor = Color.rgb(3, 13, 27)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        @Suppress("DEPRECATION")
        run { window.decorView.systemUiVisibility = 0 }
        loadSettings()
        buildScreen()
    }

    internal fun prefs() = getSharedPreferences("mdf_kar_settings", MODE_PRIVATE)

    internal fun loadSettings() {
        val p = prefs()
        english = p.getBoolean("english", false)
    }

    internal fun saveSettings() {
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
            .putInt("cabinetType", cabinetSpinner.selectedItemPosition.coerceIn(0, 2))
            .putBoolean("includeBack", includeBackCheck.isChecked)
            .putBoolean("legs", legsCheck.isChecked)
            .putBoolean("doorPvc", doorPvcCheck.isChecked)
            .putBoolean("sidePvc", sidePvcCheck.isChecked)
            .putBoolean("shelfPvc", shelfPvcCheck.isChecked)
            .apply()
    }

    internal fun saved(key: String, fallback: String) = prefs().getString(key, fallback) ?: fallback

    internal fun field(hintFa: String, hintEn: String, key: String, fallback: String): EditText {
        return EditText(this).apply {
            // Labels are displayed above each field; keep the field itself free of placeholder text.
            hint = ""
            setText(if (english) saved(key, fallback) else faDigits(saved(key, fallback)))
            textSize = 16f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            setTextColor(ink)
            setHintTextColor(muted)
            setPadding(12, 5, 12, 5)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            background = rounded(fieldFill, 12, Color.rgb(56, 73, 98), 1)
        }
    }

    internal fun tr(fa: String, en: String) = if (english) en else fa

    internal fun rounded(color: Int, radiusDp: Int, strokeColor: Int? = null, strokeDp: Int = 0): GradientDrawable =
        AppTheme.rounded(this, color, radiusDp, strokeColor, strokeDp)

    internal fun label(fa: String, en: String, size: Float = 15f): TextView {
        return TextView(this).apply {
            text = tr(fa, en)
            textSize = size
            setTextColor(if (size >= 18f) Color.rgb(106, 170, 255) else ink)
            if (size >= 18f) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 7, 0, 4)
        }
    }

    internal fun button(fa: String, en: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = tr(fa, en)
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(green, 14, Color.rgb(91, 157, 255), 1)
            setPadding(10, 6, 10, 6)
            setOnClickListener { action() }
        }
    }

    internal fun check(fa: String, en: String, key: String, default: Boolean): CheckBox {
        return CheckBox(this).apply {
            text = tr(fa, en)
            textSize = 14f
            setTextColor(ink)
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.rgb(76, 145, 255))
            isChecked = prefs().getBoolean(key, default)
        }
    }

    internal fun addField(view: View) {
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(4)
        content.addView(view, lp)
    }



    internal fun projectsDirectory(): File = File(filesDir, "projects").apply { if (!exists()) mkdirs() }

    internal fun projectValues(): JSONObject {
        return JSONObject().apply {
            put("width", widthInput.text.toString())
            put("height", heightInput.text.toString())
            put("depth", depthInput.text.toString())
            put("thickness", thicknessInput.text.toString())
            put("shelves", shelfInput.text.toString())
            put("doors", doorInput.text.toString())
            put("plinth", plinthInput.text.toString())
            put("backThickness", backThicknessInput.text.toString())
            put("cabinetQuantity", cabinetQuantityInput.text.toString())
            put("sheetWidth", sheetWidthInput.text.toString())
            put("sheetHeight", sheetHeightInput.text.toString())
            put("kerf", kerfInput.text.toString())
            put("cabinetType", cabinetSpinner.selectedItemPosition.coerceIn(0, 2))
            put("includeBack", includeBackCheck.isChecked)
            put("legs", legsCheck.isChecked)
            put("doorPvc", doorPvcCheck.isChecked)
            put("sidePvc", sidePvcCheck.isChecked)
            put("shelfPvc", shelfPvcCheck.isChecked)
            put("english", english)
        }
    }

    internal fun styleDialog(dialog: AlertDialog) {
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(rounded(Color.rgb(7, 24, 43), 20, AppTheme.CARD_BORDER, 1))
            dialog.listView?.let { list ->
                list.setBackgroundColor(Color.rgb(7, 24, 43))
                for (i in 0 until list.childCount) {
                    (list.getChildAt(i) as? TextView)?.setTextColor(AppTheme.TEXT)
                }
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(AppTheme.ACCENT)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(AppTheme.TEXT_SECONDARY)
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(AppTheme.ACCENT)
            val messageId = resources.getIdentifier("message", "id", "android")
            dialog.findViewById<TextView>(messageId)?.setTextColor(AppTheme.TEXT_SECONDARY)
            val titleId = resources.getIdentifier("alertTitle", "id", "android")
            dialog.findViewById<TextView>(titleId)?.setTextColor(AppTheme.ACCENT)
        }
    }

    internal fun saveProjectDialog() {
        val nameInput = EditText(this).apply {
            hint = tr("نام پروژه (مثلاً کابینت آشپزخانه)", "Project name (e.g. kitchen cabinet)")
            setSingleLine(true)
            setTextColor(ink)
            setHintTextColor(muted)
            background = rounded(fieldFill, 12, AppTheme.FIELD_BORDER, 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(tr("ذخیره پروژه", "Save project"))
            .setMessage(tr("برای این طرح یک نام وارد کنید.", "Enter a name for this design."))
            .setView(nameInput)
            .setNegativeButton(tr("انصراف", "Cancel"), null)
            .setPositiveButton(tr("ذخیره", "Save")) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(this, tr("نام پروژه را وارد کنید.", "Enter a project name."), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                try {
                    val safeName = name.replace(Regex("[^\\p{L}\\p{N} _.-]"), "_").take(60).ifBlank { "project" }
                    val file = File(projectsDirectory(), "$safeName.json")
                    val data = projectValues().put("projectName", name)
                    file.writeText(data.toString(2), Charsets.UTF_8)
                    saveSettings()
                    Toast.makeText(this, tr("پروژه ذخیره شد.", "Project saved."), Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this, tr("ذخیره پروژه انجام نشد.", "Could not save project."), Toast.LENGTH_LONG).show()
                }
            }
            .create()
        styleDialog(dialog)
        dialog.show()
    }

    internal fun chooseProjectToLoad() {
        val files = projectsDirectory().listFiles { file -> file.isFile && file.extension.equals("json", true) }
            ?.sortedBy { it.name.lowercase(Locale.ROOT) } ?: emptyList()
        if (files.isEmpty()) {
            Toast.makeText(this, tr("هنوز پروژه‌ای ذخیره نشده است.", "No saved projects yet."), Toast.LENGTH_SHORT).show()
            return
        }
        val names = files.map { file ->
            try { JSONObject(file.readText(Charsets.UTF_8)).optString("projectName", file.nameWithoutExtension) }
            catch (_: Exception) { file.nameWithoutExtension }
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(tr("انتخاب پروژه", "Choose a project"))
            .setItems(names.toTypedArray()) { _, which -> loadProject(files[which]) }
            .setNegativeButton(tr("انصراف", "Cancel"), null)
            .create()
        styleDialog(dialog)
        dialog.show()
    }

    internal fun manageProjectsDialog() {
        val files = projectsDirectory().listFiles { file -> file.isFile && file.extension.equals("json", true) }
            ?.sortedBy { it.name.lowercase(Locale.ROOT) } ?: emptyList()
        if (files.isEmpty()) {
            Toast.makeText(this, tr("هنوز پروژه‌ای ذخیره نشده است.", "No saved projects yet."), Toast.LENGTH_SHORT).show()
            return
        }
        val names = files.map { file ->
            try { JSONObject(file.readText(Charsets.UTF_8)).optString("projectName", file.nameWithoutExtension) }
            catch (_: Exception) { file.nameWithoutExtension }
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(tr("مدیریت پروژه‌ها", "Manage projects"))
            .setItems(names.toTypedArray()) { _, which -> projectActions(files[which], names[which]) }
            .setNegativeButton(tr("بستن", "Close"), null)
            .create()
        styleDialog(dialog)
        dialog.show()
    }

    internal fun projectActions(file: File, currentName: String) {
        AlertDialog.Builder(this)
            .setTitle(currentName)
            .setItems(arrayOf(tr("باز کردن پروژه", "Open project"), tr("تغییر نام", "Rename"), tr("حذف پروژه", "Delete project"))) { _, which ->
                when (which) {
                    0 -> loadProject(file)
                    1 -> renameProject(file, currentName)
                    2 -> AlertDialog.Builder(this)
                        .setTitle(tr("حذف پروژه", "Delete project"))
                        .setMessage(tr("پروژه «$currentName» حذف شود؟", "Delete project “$currentName”?"))
                        .setNegativeButton(tr("انصراف", "Cancel"), null)
                        .setPositiveButton(tr("حذف", "Delete")) { _, _ ->
                            if (file.delete()) Toast.makeText(this, tr("پروژه حذف شد.", "Project deleted."), Toast.LENGTH_SHORT).show()
                            else Toast.makeText(this, tr("حذف پروژه انجام نشد.", "Could not delete project."), Toast.LENGTH_LONG).show()
                        }.show()
                }
            }
            .setNegativeButton(tr("انصراف", "Cancel"), null)
            .show()
    }

    internal fun renameProject(file: File, currentName: String) {
        val input = EditText(this).apply {
            setText(currentName)
            setTextColor(ink)
            setHintTextColor(muted)
            setSingleLine(true)
            background = rounded(fieldFill, 12, Color.rgb(56, 73, 98), 1)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(tr("تغییر نام پروژه", "Rename project"))
            .setView(input)
            .setNegativeButton(tr("انصراف", "Cancel"), null)
            .setPositiveButton(tr("ذخیره", "Save")) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isBlank()) {
                    Toast.makeText(this, tr("نام پروژه را وارد کنید.", "Enter a project name."), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                try {
                    val safeName = newName.replace(Regex("[^\\p{L}\\p{N} _.-]"), "_").take(60).ifBlank { "project" }
                    val target = File(projectsDirectory(), "$safeName.json")
                    if (target.exists() && target.canonicalFile != file.canonicalFile) {
                        Toast.makeText(this, tr("پروژه‌ای با این نام وجود دارد.", "A project with this name already exists."), Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    val data = JSONObject(file.readText(Charsets.UTF_8)).put("projectName", newName)
                    val destination = if (target.canonicalFile == file.canonicalFile) file else target
                    destination.writeText(data.toString(2), Charsets.UTF_8)
                    if (destination != file) file.delete()
                    Toast.makeText(this, tr("نام پروژه تغییر کرد.", "Project renamed."), Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this, tr("تغییر نام انجام نشد.", "Could not rename project."), Toast.LENGTH_LONG).show()
                }
            }.create()
        styleDialog(dialog)
        dialog.show()
    }

    internal fun loadProject(file: File) {
        try {
            val data = JSONObject(file.readText(Charsets.UTF_8))
            fun putText(key: String, field: EditText) {
                if (data.has(key)) {
                    val raw = data.optString(key, "")
                    field.setText(if (english) raw else faDigits(raw))
                }
            }
            putText("width", widthInput)
            putText("height", heightInput)
            putText("depth", depthInput)
            putText("thickness", thicknessInput)
            putText("shelves", shelfInput)
            putText("doors", doorInput)
            putText("plinth", plinthInput)
            putText("backThickness", backThicknessInput)
            putText("cabinetQuantity", cabinetQuantityInput)
            putText("sheetWidth", sheetWidthInput)
            putText("sheetHeight", sheetHeightInput)
            putText("kerf", kerfInput)
            cabinetSpinner.setSelection((data.optInt("cabinetType", 0) + 1).coerceIn(1, 2))
            includeBackCheck.isChecked = data.optBoolean("includeBack", true)
            legsCheck.isChecked = data.optBoolean("legs", true)
            doorPvcCheck.isChecked = data.optBoolean("doorPvc", true)
            sidePvcCheck.isChecked = data.optBoolean("sidePvc", true)
            shelfPvcCheck.isChecked = data.optBoolean("shelfPvc", true)
            saveSettings()
            calculate()
            Toast.makeText(this, tr("پروژه بارگذاری شد.", "Project loaded."), Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, tr("فایل پروژه قابل خواندن نیست.", "Could not read project file."), Toast.LENGTH_LONG).show()
        }
    }

    internal fun partColor(name: String): Int {
        val key = name.lowercase(Locale.ROOT)
        return when {
            key.contains("پهلو") || key.contains("side") -> Color.rgb(55, 145, 230)
            key.contains("کف") || key.contains("bottom") -> Color.rgb(65, 185, 165)
            key.contains("طبقه") || key.contains("shelf") -> Color.rgb(155, 115, 225)
            key.contains("درب") || key.contains("door") -> Color.rgb(235, 135, 90)
            key.contains("پشت") || key.contains("back") -> Color.rgb(70, 115, 175)
            else -> Color.rgb(80, 170, 220)
        }
    }

    internal fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    internal fun number(raw: String): Double? {
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

    internal fun fmt(v: Double): String = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US)).format(v)

    internal fun faDigits(s: String): String {
        val map = charArrayOf('۰','۱','۲','۳','۴','۵','۶','۷','۸','۹')
        return s.map { if (it in '0'..'9') map[it - '0'] else it }.joinToString("")
    }

    /** Stops the layout before an oversized part can be drawn outside the sheet. */
    internal fun rejectPartsThatDoNotFit(parts: List<CuttingPart>, sheetW: Double, sheetH: Double): Boolean {
        val invalid = parts.filter { part ->
            val normalFits = part.width <= sheetW && part.height <= sheetH
            val rotatedFits = part.rotate && part.height <= sheetW && part.width <= sheetH
            !normalFits && !rotatedFits
        }
        if (invalid.isEmpty()) return false

        latestPlacements = emptyList()
        latestSheetCount = 0
        selectedSheetIndex = 0
        updateSheetNavigation()
        if (::layoutView.isInitialized) layoutView.invalidate()
        if (::metricSheets.isInitialized) metricSheets.text = if (english) "0" else "۰"
        if (::metricPieces.isInitialized) metricPieces.text = if (english) "0" else "۰"
        if (::metricWaste.isInitialized) metricWaste.text = if (english) "—" else "—"

        val details = invalid.take(8).joinToString("\n") { part ->
            val dimensions = "${fmt(part.width)} × ${fmt(part.height)} mm"
            if (english) "• ${part.name}: $dimensions" else "• ${part.name}: ${faDigits(dimensions)} میلی‌متر"
        }
        val more = if (invalid.size > 8) {
            if (english) "\n…and ${invalid.size - 8} more part types." else "\nو ${faDigits((invalid.size - 8).toString())} مورد دیگر."
        } else ""
        resultText.text = if (english) {
            "Some parts are larger than the sheet and cannot be placed:\n$details$more\n\nIncrease sheet dimensions or review the part dimensions. No cutting layout was generated."
        } else {
            "بعضی قطعات از ابعاد ورق بزرگ‌تر هستند و روی آن جا نمی‌شوند:\n$details$more\n\nابعاد ورق یا ابعاد قطعات را بررسی کنید. نقشهٔ برش ساخته نشد."
        }
        return true
    }

    internal fun calculate() {
        // Independent cutting uses the dimensions entered directly in its form; no separate
        // "custom cutting"/add-part workflow is required.
        if (cabinetSpinner.selectedItemPosition == 0) {
            val name = customPartNameInput.text.toString().trim().ifBlank { tr("قطعه مستقل", "Independent part") }
            val length = number(customPartLengthInput.text.toString())
            val width = number(customPartWidthInput.text.toString())
            val qty = number(customPartQtyInput.text.toString())?.toInt()
            val thickness = number(customPartThicknessInput.text.toString())
            if (length == null || width == null || qty == null || thickness == null || length <= 0 || width <= 0 || qty !in 1..1000 || thickness <= 0) {
                resultText.text = tr("نام قطعه، طول، عرض، ضخامت و تعداد معتبر وارد کنید.", "Enter a part name, valid length, width, thickness and quantity.")
                return
            }
            orderCabinets.clear()
            customParts.clear()
            customParts += CustomOrderPart(name, length, width, qty, thickness)
            calculateMultiCabinetOrder()
            return
        }
        if (orderCabinets.isNotEmpty() || customParts.isNotEmpty()) {
            calculateMultiCabinetOrder()
            return
        }
        val w = number(widthInput.text.toString())
        val h = number(heightInput.text.toString())
        val d = number(depthInput.text.toString())
        val t = number(thicknessInput.text.toString())
        val shelves = number(shelfInput.text.toString())?.toInt()
        val doors = number(doorInput.text.toString())?.toInt()
        val plinthInputValue = number(plinthInput.text.toString())
        val backT = number(backThicknessInput.text.toString())
        val cabinetQuantity = number(cabinetQuantityInput.text.toString())?.toInt()
        val sw = number(sheetWidthInput.text.toString())
        val sh = number(sheetHeightInput.text.toString())
        val kerf = number(kerfInput.text.toString())

        if (w == null || h == null || d == null || t == null || shelves == null ||
            doors == null || plinthInputValue == null || backT == null || cabinetQuantity == null || sw == null ||
            sh == null || kerf == null || w <= 0 || h <= 0 || d <= 0 || t <= 0 ||
            shelves !in 0..30 || doors !in 0..2 || plinthInputValue < 0 || backT < 0 || cabinetQuantity !in 1..100 ||
            sw <= 0 || sh <= 0 || kerf < 0) {
            resultText.text = tr(
                "ابعاد را معتبر وارد کنید. تعداد طبقه ۰ تا ۳۰، تعداد در ۰ تا ۲ و تعداد کابینت ۱ تا ۱۰۰ باشد.",
                "Enter valid dimensions. Shelves: 0–30, doors: 0–2, cabinet quantity: 1–100."
            )
            return
        }

        val isBase = cabinetSpinner.selectedItemPosition != 2
        val plinth = if (isBase && legsCheck.isChecked) plinthInputValue else 0.0
        val bodyH = h - plinth
        val innerW = w - 2 * t
        val innerD = d
        if (bodyH <= 0 || innerW <= 0 || innerD <= 0) {
            resultText.text = tr("ابعاد پس از کسر ضخامت/پاخور معتبر نیست.", "Dimensions are invalid after subtracting board thickness/plinth.")
            return
        }

        val parts = mutableListOf<CuttingPart>()
        parts += CuttingPart(tr("پهلو", "Side"), d, bodyH, 2)
        parts += CuttingPart(tr("کف", "Bottom"), innerW, d)
        if (shelves > 0) parts += CuttingPart(tr("طبقه", "Shelf"), innerW, d, shelves)
        if (isBase) {
            parts += CuttingPart(tr("ریل بالا", "Top rail"), max(100.0, innerW / 8), t, 2)
        } else {
            parts += CuttingPart(tr("بالا", "Top"), innerW, d)
        }

        if (includeBackCheck.isChecked) {
            parts += CuttingPart(tr("پشت‌بند", "Back panel"), w, bodyH, 1, false)
        }
        if (doors > 0) {
            val doorW = (w - 3.0) / doors
            val doorH = bodyH - 3.0
            if (doorW > 0 && doorH > 0) parts += CuttingPart(tr("درب", "Door"), doorW, doorH, doors)
        }

        val multipliedParts = parts.map { it.copy(quantity = it.quantity * cabinetQuantity) }
        val boardParts = multipliedParts.filter { it.name != tr("پشت‌بند", "Back panel") }
        if (rejectPartsThatDoNotFit(boardParts, sw, sh)) return
        val layout = CuttingCalculator.packParts(boardParts, sw, sh, kerf)
        latestPlacements = layout.placements
        latestSheetW = sw
        latestSheetH = sh
        latestSheetCount = layout.sheetCount
        selectedSheetIndex = 0
        updateSheetNavigation()
        layoutView.invalidate()

        val boardArea = boardParts.sumOf { it.width * it.height * it.quantity }
        val sheetArea = sw * sh
        val sheetCount = layout.sheetCount
        metricSheets.text = if (english) sheetCount.toString() else faDigits(sheetCount.toString())
        val unusedArea = max(0.0, sheetCount * sheetArea - boardArea)
        val wastePercent = if (sheetCount > 0) unusedArea / (sheetCount * sheetArea) * 100.0 else 0.0
        metricWaste.text = if (english) "${fmt(wastePercent)}%" else faDigits("${fmt(wastePercent)}٪")
        metricPieces.text = if (english) layout.placements.size.toString() else faDigits(layout.placements.size.toString())
        val backArea = if (includeBackCheck.isChecked) w * bodyH * cabinetQuantity / 1_000_000.0 else 0.0

        var pvcMeters = 0.0
        if (doorPvcCheck.isChecked && doors > 0) {
            val doorW = (w - 3.0) / doors
            val doorH = bodyH - 3.0
            pvcMeters += cabinetQuantity * doors * 2 * (doorW + doorH) / 1000.0
        }
        if (sidePvcCheck.isChecked) pvcMeters += cabinetQuantity * 2 * bodyH / 1000.0
        if (shelfPvcCheck.isChecked) pvcMeters += cabinetQuantity * (1 + shelves) * innerW / 1000.0

        partsList.removeAllViews()
        multipliedParts.forEachIndexed { index, part ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(7), dp(8), dp(7), dp(8))
                if (index % 2 == 0) setBackgroundColor(Color.rgb(8, 30, 51))
            }
            row.addView(View(this).apply {
                background = rounded(partColor(part.name), 5)
                layoutParams = LinearLayout.LayoutParams(dp(12), dp(28)).apply { marginEnd = dp(8) }
            })
            row.addView(TextView(this).apply {
                text = part.name
                textSize = 12f
                setTextColor(ink)
                gravity = if (english) Gravity.START else Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, -2, 1.2f)
            })
            row.addView(TextView(this).apply {
                val dims = "${fmt(part.width)} × ${fmt(part.height)}"
                text = if (english) "$dims mm" else "${faDigits(dims)} میلی‌متر"
                textSize = 11f
                setTextColor(AppTheme.TEXT_SECONDARY)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, -2, 1.5f)
            })
            row.addView(TextView(this).apply {
                text = if (english) part.quantity.toString() else faDigits(part.quantity.toString())
                textSize = 12f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(32), -2)
            })
            partsList.addView(row)
            if (index < parts.lastIndex) partsList.addView(View(this).apply { setBackgroundColor(Color.rgb(28, 61, 89)); layoutParams = LinearLayout.LayoutParams(-1, dp(1)) })
        }
        val summaryFa = """
            جزئیات برش:
            تعداد کابینت مشابه: $cabinetQuantity
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
            Cutting details:
            Identical cabinets: $cabinetQuantity
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

    internal fun refreshOrderList() {
        if (!::orderListView.isInitialized) return
        if (::customPartCabinetSpinner.isInitialized) {
            customPartCabinetSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
                if (orderCabinets.isEmpty()) listOf(tr("ابتدا کابینت اضافه کنید", "Add a cabinet first")) else orderCabinets.map { it.name })
        }
        orderListView.removeAllViews()
        if (orderCabinets.isEmpty()) {
            orderListView.addView(TextView(this).apply {
                text = tr("هنوز کابینتی به سفارش اضافه نشده است.", "No cabinets have been added yet.")
                textSize = 11f; setTextColor(muted); setPadding(dp(8), dp(5), dp(8), dp(5))
            })
            return
        }
        orderCabinets.forEachIndexed { index, c ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(9), dp(7), dp(9), dp(7))
                background = rounded(Color.rgb(10, 39, 65), 10, Color.rgb(35, 100, 150), 1)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(5) }
            }
            val info = TextView(this).apply {
                text = "${c.name}  •  ${fmt(c.widthMm)}×${fmt(c.heightMm)}×${fmt(c.depthMm)} mm  •  ${tr("ضخامت", "thickness")} ${fmt(c.boardThicknessMm)}  •  ${tr("تعداد", "qty")} ${c.quantity}"
                textSize = 11f; setTextColor(ink)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            row.addView(info)
            row.addView(button("×", "×") {
                orderCabinets.removeAt(index); refreshOrderList()
            }.apply { minWidth = dp(40); minHeight = dp(38) })
            orderListView.addView(row)
        }
    }

    internal fun refreshCustomPartsList() {
        if (!::customPartsListView.isInitialized) return
        customPartsListView.removeAllViews()
        updateCustomPartsToggleLabel()
        if (customParts.isEmpty()) {
            customPartsListView.addView(TextView(this).apply {
                text = tr("هنوز قطعه سفارشی اضافه نشده است.", "No custom parts added yet.")
                textSize = 11f; setTextColor(muted); setPadding(dp(8), dp(5), dp(8), dp(5))
            })
            return
        }
        customParts.forEachIndexed { index, part ->
            val linkedName = part.linkedCabinetId?.let { id -> orderCabinets.find { it.id == id }?.name }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(9), dp(7), dp(9), dp(7))
                background = rounded(Color.rgb(22, 48, 63), 10, Color.rgb(45, 125, 145), 1)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(5) }
            }
            val info = TextView(this).apply {
                text = "${part.name} • ${fmt(part.lengthMm)}×${fmt(part.widthMm)} mm • ${tr("تعداد", "qty")} ${part.quantity} • ${fmt(part.thicknessMm)} mm${if (linkedName != null) " • $linkedName" else " • ${tr("مستقل", "standalone")}"}"
                textSize = 10f; setTextColor(ink); layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            row.addView(info)
            row.addView(button("×", "×") {
                customParts.removeAt(index); refreshCustomPartsList()
            }.apply { minWidth = dp(40); minHeight = dp(38) })
            customPartsListView.addView(row)
        }
    }

    internal fun updateCustomPartsToggleLabel() {
        if (!::customPartsToggleButton.isInitialized) return
        val expanded = ::customPartsSection.isInitialized && customPartsSection.visibility == View.VISIBLE
        val prefix = if (expanded) "−" else "+"
        customPartsToggleButton.text = if (english) "$prefix Custom parts (${customParts.size})" else "$prefix قطعات سفارشی (${faDigits(customParts.size.toString())})"
    }

    internal fun calculateMultiCabinetOrder() {
        val sw = number(sheetWidthInput.text.toString())
        val sh = number(sheetHeightInput.text.toString())
        val kerf = number(kerfInput.text.toString())
        if (sw == null || sh == null || kerf == null || sw <= 0 || sh <= 0 || kerf < 0 || orderCabinets.any { !it.isValid() }) {
            resultText.text = tr("ابعاد ورق یا اطلاعات سفارش معتبر نیست.", "Sheet dimensions or order details are invalid.")
            return
        }
        val allParts = mutableListOf<CuttingPart>()
        val partsByStock = linkedMapOf<String, MutableList<CuttingPart>>()
        var backArea = 0.0
        var pvcMeters = 0.0
        orderCabinets.forEach { c ->
            val isBase = c.cabinetType == CabinetType.BASE
            val plinth = if (isBase && legsCheck.isChecked) c.plinthMm else 0.0
            val bodyH = c.heightMm - plinth
            val innerW = c.widthMm - 2 * c.boardThicknessMm
            if (bodyH <= 0 || innerW <= 0) {
                resultText.text = tr("ابعاد ${c.name} پس از کسر ضخامت/پاخور معتبر نیست.", "Dimensions for ${c.name} are invalid after subtracting thickness/plinth.")
                return
            }
            val stockKey = "${c.stockType}|${c.boardThicknessMm}"
            val stockParts = partsByStock.getOrPut(stockKey) { mutableListOf() }
            fun part(labelFa: String, labelEn: String, w: Double, h: Double, qty: Int, rotate: Boolean = false) {
                if (qty > 0 && w > 0 && h > 0) {
                    val item = CuttingPart("${c.name} - ${tr(labelFa, labelEn)}", w, h, qty * c.quantity, rotate)
                    allParts += item
                    stockParts += item
                }
            }
            part("پهلو", "Side", c.depthMm, bodyH, 2)
            part("کف", "Bottom", innerW, c.depthMm, 1)
            part("طبقه", "Shelf", innerW, c.depthMm, c.shelfCount)
            if (isBase) part("ریل بالا", "Top rail", max(100.0, innerW / 8), c.boardThicknessMm, 2, true)
            else part("بالا", "Top", innerW, c.depthMm, 1)
            if (c.doorCount > 0) part("درب", "Door", (c.widthMm - 3.0) / c.doorCount, bodyH - 3.0, c.doorCount)
            if (c.includeBack && includeBackCheck.isChecked) backArea += c.widthMm * bodyH * c.quantity / 1_000_000.0
            if (doorPvcCheck.isChecked && c.doorCount > 0) {
                val dw = (c.widthMm - 3.0) / c.doorCount
                pvcMeters += c.quantity * c.doorCount * 2 * (dw + bodyH) / 1000.0
            }
            if (sidePvcCheck.isChecked) pvcMeters += c.quantity * 2 * bodyH / 1000.0
            if (shelfPvcCheck.isChecked) pvcMeters += c.quantity * (1 + c.shelfCount) * innerW / 1000.0
        }
        // Add standalone or cabinet-linked custom parts to the matching stock/thickness group.
        customParts.forEach { custom ->
            val cabinet = custom.linkedCabinetId?.let { id -> orderCabinets.find { it.id == id } }
            val referenceLength = if (cabinet == null || custom.referenceDimension < 0) custom.lengthMm else when (custom.referenceDimension) {
                0 -> cabinet.widthMm
                1 -> cabinet.heightMm
                else -> cabinet.depthMm
            }
            val quantity = custom.quantity * (cabinet?.quantity ?: 1)
            val item = CuttingPart("${custom.name}${cabinet?.let { " - ${it.name}" } ?: " - ${tr("سفارشی", "Custom")}"}", referenceLength, custom.widthMm, quantity, custom.linkedCabinetId == null)
            allParts += item
            val stockKey = "MDF|${custom.thicknessMm}"
            partsByStock.getOrPut(stockKey) { mutableListOf() }.add(item)
        }
        // Reject oversized parts before the packer can draw an invalid overflow placement.
        val allStockParts = partsByStock.values.flatten()
        if (rejectPartsThatDoNotFit(allStockParts, sw, sh)) return

        // Each material/thickness group is laid out separately.
        val placements = mutableListOf<SheetPlacement>()
        var sheetOffset = 0
        partsByStock.values.forEach { parts ->
            if (parts.isNotEmpty()) {
                val packed = CuttingCalculator.packParts(parts, sw, sh, kerf)
                placements += packed.placements.map { it.copy(sheet = it.sheet + sheetOffset) }
                sheetOffset += packed.sheetCount
            }
        }
        latestPlacements = placements
        latestSheetW = sw; latestSheetH = sh; latestSheetCount = sheetOffset; selectedSheetIndex = 0
        updateSheetNavigation(); layoutView.invalidate()
        val boardArea = allParts.sumOf { it.width * it.height * it.quantity }
        val totalSheetArea = latestSheetCount * sw * sh
        val waste = if (totalSheetArea > 0) max(0.0, (totalSheetArea - boardArea) / totalSheetArea * 100.0) else 0.0
        metricSheets.text = if (english) latestSheetCount.toString() else faDigits(latestSheetCount.toString())
        metricWaste.text = if (english) "${fmt(waste)}%" else faDigits("${fmt(waste)}٪")
        metricPieces.text = if (english) placements.size.toString() else faDigits(placements.size.toString())
        partsList.removeAllViews()
        allParts.forEachIndexed { index, part ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(7), dp(8), dp(7), dp(8)); if (index % 2 == 0) setBackgroundColor(Color.rgb(8, 30, 51)) }
            row.addView(View(this).apply { background = rounded(partColor(part.name), 5); layoutParams = LinearLayout.LayoutParams(dp(12), dp(28)).apply { marginEnd = dp(8) } })
            row.addView(TextView(this).apply { text = part.name; textSize = 11f; setTextColor(ink); layoutParams = LinearLayout.LayoutParams(0, -2, 1.4f) })
            row.addView(TextView(this).apply { text = "${faDigits(fmt(part.width))} × ${faDigits(fmt(part.height))}"; textSize = 10f; setTextColor(AppTheme.TEXT_SECONDARY); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, -2, 1.1f) })
            row.addView(TextView(this).apply { text = faDigits(part.quantity.toString()); textSize = 11f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(dp(30), -2) })
            partsList.addView(row)
        }
        val cabinetSummary = orderCabinets.joinToString("\n") { c -> "${c.name}: ${fmt(c.widthMm)}×${fmt(c.heightMm)}×${fmt(c.depthMm)} mm, ${tr("تعداد", "qty")} ${c.quantity}, ${tr("ضخامت", "thickness")} ${fmt(c.boardThicknessMm)} mm" }
        resultText.text = if (english) "Multi-cabinet order\n$cabinetSummary\nSheets: $latestSheetCount\nEstimated area-based waste: ${fmt(waste)}%\nSeparate back panels: ${fmt(backArea)} m²\nEstimated PVC: ${fmt(pvcMeters)} m\n\nWarning: planning-level row packing; verify all dimensions and grain direction before cutting." else "سفارش چند کابینتی\n$cabinetSummary\nتعداد ورق: ${faDigits(latestSheetCount.toString())}\nدورریز تخمینی بر اساس مساحت: ${faDigits(fmt(waste))}٪\nپشت‌بند جداگانه: ${faDigits(fmt(backArea))} مترمربع\nPVC تخمینی: ${faDigits(fmt(pvcMeters))} متر\n\nهشدار: چیدمان تخمینی است؛ پیش از برش، ابعاد و جهت رگه را بررسی کنید."
    }

    internal fun updateSheetNavigation() {
        if (!::sheetCounterLabel.isInitialized) return
        val total = latestSheetCount
        sheetCounterLabel.text = if (total <= 0) tr("پس از محاسبه", "Calculate first") else {
            val current = selectedSheetIndex + 1
            if (english) "Sheet $current of $total" else "ورق ${faDigits(current.toString())} از ${faDigits(total.toString())}"
        }
        if (::previousSheetButton.isInitialized) previousSheetButton.isEnabled = selectedSheetIndex > 0 && total > 1
        if (::nextSheetButton.isInitialized) nextSheetButton.isEnabled = selectedSheetIndex < total - 1 && total > 1
    }


}
