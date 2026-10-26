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
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
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

class MainActivity : Activity() {

    // Visual tokens live in AppTheme.kt so future appearance changes stay separate.
    private val dark = AppTheme.BACKGROUND_TOP
    private val green = AppTheme.PRIMARY
    private val pale = AppTheme.BACKGROUND
    private val ink = AppTheme.TEXT
    private val card = AppTheme.CARD
    private val fieldFill = AppTheme.FIELD
    private val muted = AppTheme.MUTED

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
            setHintTextColor(muted)
            setPadding(16, 8, 16, 8)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            background = rounded(fieldFill, 12, Color.rgb(56, 73, 98), 1)
        }
    }

    private fun tr(fa: String, en: String) = if (english) en else fa

    private fun rounded(color: Int, radiusDp: Int, strokeColor: Int? = null, strokeDp: Int = 0): GradientDrawable =
        AppTheme.rounded(this, color, radiusDp, strokeColor, strokeDp)

    private fun label(fa: String, en: String, size: Float = 15f): TextView {
        return TextView(this).apply {
            text = tr(fa, en)
            textSize = size
            setTextColor(if (size >= 18f) Color.rgb(106, 170, 255) else ink)
            if (size >= 18f) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 12, 0, 8)
        }
    }

    private fun button(fa: String, en: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = tr(fa, en)
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(green, 14, Color.rgb(91, 157, 255), 1)
            setPadding(12, 10, 12, 10)
            setOnClickListener { action() }
        }
    }

    private fun check(fa: String, en: String, key: String, default: Boolean): CheckBox {
        return CheckBox(this).apply {
            text = tr(fa, en)
            textSize = 14f
            setTextColor(ink)
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.rgb(76, 145, 255))
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

        val scroll = ScrollView(this).apply { clipToPadding = false }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(20))
            background = rounded(pale, 0)
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // Hero banner: a real layout component, not just a recolored title bar.
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(22))
            background = AppTheme.heroBackground(this@MainActivity)
            elevation = dp(3).toFloat()
        }
        val heroTop = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = TextView(this).apply {
            text = "MDF-KAR"
            textSize = 30f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            letterSpacing = 0.04f
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val languageButton = TextView(this).apply {
            text = if (english) "فارسی" else "EN"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = rounded(Color.rgb(18, 48, 83), 18, AppTheme.ACCENT, 1)
            setOnClickListener {
                saveSettings()
                english = !english
                prefs().edit().putBoolean("english", english).apply()
                buildScreen()
            }
        }
        heroTop.addView(brand)
        heroTop.addView(languageButton)
        hero.addView(heroTop)
        hero.addView(TextView(this).apply {
            text = tr("محاسبه و چیدمان هوشمند ورق MDF", "MDF cabinet calculator & cut planner")
            textSize = 15f
            setTextColor(AppTheme.TEXT_SECONDARY)
            setPadding(0, dp(4), 0, dp(12))
        })
        val heroFoot = TextView(this).apply {
            text = tr("طراحی دقیق‌تر  •  برش بهینه‌تر  •  مدیریت پروژه‌ها", "Plan smarter  •  Cut cleaner  •  Manage projects")
            textSize = 12f
            setTextColor(AppTheme.ACCENT)
            setPadding(0, dp(8), 0, 0)
        }
        hero.addView(heroFoot)
        val heroParams = LinearLayout.LayoutParams(-1, -2)
        heroParams.bottomMargin = dp(16)
        content.addView(hero, heroParams)

        fun section(titleFa: String, titleEn: String, subtitleFa: String? = null, subtitleEn: String? = null): LinearLayout {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(8), dp(15), dp(14))
                background = rounded(card, 18, AppTheme.CARD_BORDER, 1)
                elevation = dp(2).toFloat()
            }
            val params = LinearLayout.LayoutParams(-1, -2)
            params.bottomMargin = dp(13)
            content.addView(box, params)
            box.addView(TextView(this).apply {
                text = tr(titleFa, titleEn)
                textSize = 17f
                setTextColor(AppTheme.ACCENT)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, dp(5), 0, dp(3))
            })
            if (subtitleFa != null && subtitleEn != null) box.addView(TextView(this).apply {
                text = tr(subtitleFa, subtitleEn)
                textSize = 12f
                setTextColor(muted)
                setPadding(0, 0, 0, dp(8))
            })
            return box
        }
        fun addTo(target: LinearLayout, view: View) {
            val params = LinearLayout.LayoutParams(-1, -2)
            params.bottomMargin = dp(7)
            target.addView(view, params)
        }

        val dimensionsCard = section("ابعاد ورق و تیغه", "Sheet & saw settings", "اندازه‌ها را بر حسب میلی‌متر وارد کنید", "Enter all dimensions in millimeters")
        val sheetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val sheetWidthBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        val sheetHeightBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        sheetWidthInput = field("عرض ورق", "Sheet width", "sheetWidth", "2070")
        sheetHeightInput = field("طول ورق", "Sheet length", "sheetHeight", "2800")
        sheetWidthBox.addView(label("عرض ورق (میلی‌متر)", "Sheet width (mm)", 12f)); sheetWidthBox.addView(sheetWidthInput)
        sheetHeightBox.addView(label("طول ورق (میلی‌متر)", "Sheet length (mm)", 12f)); sheetHeightBox.addView(sheetHeightInput)
        sheetRow.addView(sheetWidthBox)
        val rowGap = View(this); rowGap.layoutParams = LinearLayout.LayoutParams(dp(10), 1)
        sheetRow.addView(rowGap); sheetRow.addView(sheetHeightBox)
        addTo(dimensionsCard, sheetRow)
        kerfInput = field("عرض تیغه (پیش‌فرض ۳)", "Saw kerf (default 3)", "kerf", "3")
        addTo(dimensionsCard, label("ضخامت تیغه برش (میلی‌متر)", "Saw kerf (mm)", 12f)); addTo(dimensionsCard, kerfInput)

        val cabinetCard = section("مشخصات کابینت", "Cabinet specifications", "نوع کابینت و اندازه‌های بدنه", "Cabinet type and carcass dimensions")
        cabinetSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                if (english) arrayOf("Base cabinet", "Wall cabinet") else arrayOf("کابینت زمینی", "کابینت دیواری"))
            setSelection(prefs().getInt("cabinetType", 0).coerceIn(0, 1))
            background = rounded(fieldFill, 12, AppTheme.FIELD_BORDER, 1)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        addTo(cabinetCard, cabinetSpinner)
        widthInput = field("عرض کابینت", "Cabinet width", "width", "800")
        heightInput = field("ارتفاع کابینت", "Cabinet height", "height", "720")
        depthInput = field("عمق کابینت", "Cabinet depth", "depth", "560")
        thicknessInput = field("ضخامت ورق بدنه", "Carcass board thickness", "thickness", "16")
        shelfInput = field("تعداد طبقه داخلی", "Number of shelves", "shelves", "1")
        doorInput = field("تعداد در (۰ تا ۲)", "Number of doors (0–2)", "doors", "2")
        plinthInput = field("ارتفاع پایه/پاخور", "Plinth / leg height", "plinth", "100")
        backThicknessInput = field("ضخامت پشت‌بند", "Back panel thickness", "backThickness", "3")
        val cabinetFields = listOf(
            widthInput to Pair("عرض کابینت (میلی‌متر)", "Cabinet width (mm)"),
            heightInput to Pair("ارتفاع کابینت (میلی‌متر)", "Cabinet height (mm)"),
            depthInput to Pair("عمق کابینت (میلی‌متر)", "Cabinet depth (mm)"),
            thicknessInput to Pair("ضخامت ورق بدنه", "Carcass board thickness"),
            shelfInput to Pair("تعداد طبقه", "Number of shelves"),
            doorInput to Pair("تعداد در", "Number of doors"),
            plinthInput to Pair("ارتفاع پاخور", "Plinth height"),
            backThicknessInput to Pair("ضخامت پشت‌بند", "Back panel thickness")
        )
        cabinetFields.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            pair.forEachIndexed { index, item ->
                val cell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
                cell.addView(label(item.second.first, item.second.second, 12f))
                cell.addView(item.first)
                row.addView(cell)
                if (index == 0 && pair.size > 1) row.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(10), 1) })
            }
            addTo(cabinetCard, row)
        }

        val optionsCard = section("تنظیمات تکمیلی", "Additional options", "محاسبه پشت‌بند و لبه‌چسبانی PVC", "Back panel and PVC edge-banding options")
        includeBackCheck = check("محاسبه پشت‌بند", "Include back panel", "includeBack", true)
        legsCheck = check("کسر ارتفاع پاخور از بدنه کابینت زمینی", "Subtract plinth from base cabinet body", "legs", true)
        doorPvcCheck = check("PVC چهار لبه درها", "PVC on all door edges", "doorPvc", true)
        sidePvcCheck = check("PVC لبه جلویی دو پهلو", "PVC on front edges of both sides", "sidePvc", true)
        shelfPvcCheck = check("PVC لبه جلویی کف و طبقه‌ها", "PVC on front edges of bottom and shelves", "shelfPvc", true)
        listOf(includeBackCheck, legsCheck, doorPvcCheck, sidePvcCheck, shelfPvcCheck).forEach { addTo(optionsCard, it) }

        val projectCard = section("پروژه‌ها", "Projects", "طرح‌ها را ذخیره کنید و بعداً ادامه دهید", "Save designs and continue later")
        addTo(projectCard, button("ذخیره پروژه", "Save project") { saveProjectDialog() })
        addTo(projectCard, button("باز کردن پروژه ذخیره‌شده", "Open saved project") { chooseProjectToLoad() })
        addTo(projectCard, button("مدیریت پروژه‌ها", "Manage projects") { manageProjectsDialog() })

        val resultCard = section("نتیجه و چیدمان برش", "Results & cutting layout", "ابتدا محاسبه را اجرا کنید تا نقشه و فهرست قطعات نمایش داده شود", "Calculate to display the layout and parts list")
        val calculateButton = button("محاسبه قطعات و نمایش نتیجه", "Calculate parts & show results") {
            saveSettings()
            calculate()
            scroll.post { scroll.smoothScrollTo(0, resultCard.top) }
        }.apply {
            textSize = 16f
            minHeight = dp(54)
            background = AppTheme.primaryButton(this@MainActivity)
        }
        addTo(resultCard, calculateButton)
        layoutView = SheetLayoutView().apply { background = rounded(card, 15, AppTheme.CARD_BORDER, 1) }
        addTo(resultCard, label("نقشه چیدمان ورق", "Sheet cutting layout", 14f))
        resultCard.addView(layoutView, LinearLayout.LayoutParams(-1, dp(230)))
        resultText = TextView(this).apply {
            text = tr("ابعاد را وارد کنید و محاسبه را بزنید.", "Enter dimensions and calculate.")
            textSize = 14f
            setTextColor(ink)
            setPadding(dp(4), dp(12), dp(4), dp(12))
            background = rounded(fieldFill, 12, AppTheme.FIELD_BORDER, 1)
        }
        addTo(resultCard, resultText)
        addTo(resultCard, TextView(this).apply {
            text = tr("توجه: این چیدمان تخمینی است؛ پیش از برش واقعی، ابعاد، جهت رگه، لبه‌چسبانی و تلرانس دستگاه را بررسی کنید.", "Warning: estimated layout only. Verify dimensions, grain direction, edge banding and machine tolerances before cutting.")
            textSize = 11f
            setTextColor(muted)
            setPadding(dp(3), dp(8), dp(3), dp(4))
        })

        // Fixed bottom navigation gives the screen the same clear app-like hierarchy as the reference.
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(7), dp(6), dp(7))
            background = AppTheme.navBackground(this@MainActivity)
            elevation = dp(10).toFloat()
        }
        fun navItem(icon: String, fa: String, en: String, target: View, isActive: Boolean = false) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(3), dp(3), dp(3))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val y = target.top - dp(8)
                    scroll.smoothScrollTo(0, y.coerceAtLeast(0))
                }
            }
            item.addView(TextView(this).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(if (isActive) AppTheme.ACCENT else muted)
            })
            item.addView(TextView(this).apply {
                text = tr(fa, en)
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(if (isActive) AppTheme.ACCENT else muted)
            })
            nav.addView(item)
        }
        navItem("⌂", "خانه", "Home", hero, true)
        navItem("▤", "نتایج", "Results", resultCard)
        navItem("▣", "پروژه‌ها", "Projects", projectCard)
        navItem("⚙", "تنظیمات", "Settings", optionsCard)
        root.addView(nav, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }


    private fun projectsDirectory(): File = File(filesDir, "projects").apply { if (!exists()) mkdirs() }

    private fun projectValues(): JSONObject {
        return JSONObject().apply {
            put("width", widthInput.text.toString())
            put("height", heightInput.text.toString())
            put("depth", depthInput.text.toString())
            put("thickness", thicknessInput.text.toString())
            put("shelves", shelfInput.text.toString())
            put("doors", doorInput.text.toString())
            put("plinth", plinthInput.text.toString())
            put("backThickness", backThicknessInput.text.toString())
            put("sheetWidth", sheetWidthInput.text.toString())
            put("sheetHeight", sheetHeightInput.text.toString())
            put("kerf", kerfInput.text.toString())
            put("cabinetType", cabinetSpinner.selectedItemPosition)
            put("includeBack", includeBackCheck.isChecked)
            put("legs", legsCheck.isChecked)
            put("doorPvc", doorPvcCheck.isChecked)
            put("sidePvc", sidePvcCheck.isChecked)
            put("shelfPvc", shelfPvcCheck.isChecked)
            put("english", english)
        }
    }

    private fun saveProjectDialog() {
        val nameInput = EditText(this).apply {
            hint = tr("نام پروژه (مثلاً کابینت آشپزخانه)", "Project name (e.g. kitchen cabinet)")
            setSingleLine(true)
            setTextColor(ink)
        }
        AlertDialog.Builder(this)
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
            .show()
    }

    private fun chooseProjectToLoad() {
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
        AlertDialog.Builder(this)
            .setTitle(tr("انتخاب پروژه", "Choose a project"))
            .setItems(names.toTypedArray()) { _, which -> loadProject(files[which]) }
            .setNegativeButton(tr("انصراف", "Cancel"), null)
            .show()
    }

    private fun manageProjectsDialog() {
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
        AlertDialog.Builder(this)
            .setTitle(tr("مدیریت پروژه‌ها", "Manage projects"))
            .setItems(names.toTypedArray()) { _, which -> projectActions(files[which], names[which]) }
            .setNegativeButton(tr("بستن", "Close"), null)
            .show()
    }

    private fun projectActions(file: File, currentName: String) {
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

    private fun renameProject(file: File, currentName: String) {
        val input = EditText(this).apply {
            setText(currentName)
            setTextColor(ink)
            setHintTextColor(muted)
            setSingleLine(true)
            background = rounded(fieldFill, 12, Color.rgb(56, 73, 98), 1)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        AlertDialog.Builder(this)
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
            }.show()
    }

    private fun loadProject(file: File) {
        try {
            val data = JSONObject(file.readText(Charsets.UTF_8))
            fun putText(key: String, field: EditText) {
                if (data.has(key)) field.setText(data.optString(key, ""))
            }
            putText("width", widthInput)
            putText("height", heightInput)
            putText("depth", depthInput)
            putText("thickness", thicknessInput)
            putText("shelves", shelfInput)
            putText("doors", doorInput)
            putText("plinth", plinthInput)
            putText("backThickness", backThicknessInput)
            putText("sheetWidth", sheetWidthInput)
            putText("sheetHeight", sheetHeightInput)
            putText("kerf", kerfInput)
            cabinetSpinner.setSelection(data.optInt("cabinetType", 0).coerceIn(0, 1))
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

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(AppTheme.FIELD)
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
                Color.rgb(55, 145, 230), Color.rgb(65, 185, 165),
                Color.rgb(155, 115, 225), Color.rgb(235, 135, 90),
                Color.rgb(70, 115, 175), Color.rgb(80, 170, 220)
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
