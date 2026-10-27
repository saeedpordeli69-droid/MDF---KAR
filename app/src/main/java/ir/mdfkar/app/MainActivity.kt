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
    private lateinit var nav: LinearLayout
    private lateinit var metricSheets: TextView
    private lateinit var metricWaste: TextView
    private lateinit var metricPieces: TextView
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
            setText(if (english) saved(key, fallback) else faDigits(saved(key, fallback)))
            textSize = 16f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
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
            setBackgroundColor(Color.rgb(3, 13, 27))
            layoutDirection = if (english) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        }

        val pageHost = android.widget.FrameLayout(this)
        val pages = mutableListOf<LinearLayout>()
        fun makePage(): LinearLayout {
            val pageContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(28))
            }
            val scroll = ScrollView(this).apply {
                clipToPadding = false
                isFillViewport = true
                addView(pageContent)
            }
            pageHost.addView(scroll, android.widget.FrameLayout.LayoutParams(-1, -1))
            pages.add(pageContent)
            return pageContent
        }
        val homePage = makePage()
        val resultsPage = makePage()
        val projectsPage = makePage()
        val settingsPage = makePage()
        val pageViews = (0 until pageHost.childCount).map { pageHost.getChildAt(it) }
        root.addView(pageHost, LinearLayout.LayoutParams(-1, 0, 1f))

        fun showPage(index: Int) {
            pageViews.forEachIndexed { i, view -> view.visibility = if (i == index) View.VISIBLE else View.GONE }
            if (index == 1) layoutView.invalidate()
            // Keep the selected tab visibly highlighted.
            for (i in 0 until nav.childCount) {
                val item = nav.getChildAt(i) as LinearLayout
                val active = i == index
                (item.getChildAt(0) as TextView).setTextColor(if (active) AppTheme.ACCENT else muted)
                (item.getChildAt(1) as TextView).setTextColor(if (active) AppTheme.ACCENT else muted)
                item.background = if (active) rounded(Color.rgb(12, 42, 76), 14, Color.rgb(20, 113, 205), 1) else rounded(Color.TRANSPARENT, 14)
            }
        }

        fun addSection(target: LinearLayout, titleFa: String, titleEn: String, subtitleFa: String? = null, subtitleEn: String? = null): LinearLayout {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(11), dp(15), dp(15))
                background = rounded(Color.rgb(7, 27, 49), 18, Color.rgb(30, 91, 145), 1)
                elevation = dp(2).toFloat()
            }
            val params = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(13) }
            target.addView(box, params)
            box.addView(TextView(this).apply {
                text = "◆  ${tr(titleFa, titleEn)}"
                textSize = 16f
                setTextColor(Color.rgb(61, 174, 255))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, dp(2), 0, dp(7))
            })
            if (subtitleFa != null && subtitleEn != null) box.addView(TextView(this).apply {
                text = tr(subtitleFa, subtitleEn)
                textSize = 11f
                setTextColor(muted)
                setPadding(0, 0, 0, dp(8))
            })
            return box
        }
        fun addTo(target: LinearLayout, view: View, bottom: Int = 7) {
            target.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) })
        }
        fun fieldCell(labelFa: String, labelEn: String, input: EditText): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            addView(label(labelFa, labelEn, 12f))
            addView(input)
        }
        fun pairedRow(target: LinearLayout, left: LinearLayout, right: LinearLayout) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(left)
            row.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(10), 1) })
            row.addView(right)
            addTo(target, row, 8)
        }
        fun addPageHeading(target: LinearLayout, titleFa: String, titleEn: String, subFa: String, subEn: String) {
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
                background = AppTheme.heroBackground(this@MainActivity)
                elevation = dp(3).toFloat()
            }
            header.addView(TextView(this).apply {
                text = "MDF-KAR"
                textSize = 27f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                letterSpacing = 0.035f
            })
            header.addView(TextView(this).apply {
                text = tr(titleFa, titleEn)
                textSize = 16f
                setTextColor(Color.rgb(70, 181, 255))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, dp(3), 0, dp(3))
            })
            header.addView(TextView(this).apply {
                text = tr(subFa, subEn)
                textSize = 12f
                setTextColor(AppTheme.TEXT_SECONDARY)
            })
            addTo(target, header, 14)
        }

        // HOME: only the inputs needed to describe a cabinet. Results and advanced options are separate tabs.
        addPageHeading(homePage, "محاسبه کابینت", "Cabinet calculator", "ابعاد را وارد کنید و نتیجه برش را جداگانه ببینید", "Enter dimensions; view the cutting plan on its own screen")
        val sheetCard = addSection(homePage, "ابعاد ورق MDF", "MDF sheet dimensions", "همه اندازه‌ها بر حسب میلی‌متر", "All measurements in millimeters")
        sheetWidthInput = field("عرض ورق", "Sheet width", "sheetWidth", "۲۰۷۰")
        sheetHeightInput = field("طول ورق", "Sheet length", "sheetHeight", "۲۸۰۰")
        pairedRow(sheetCard, fieldCell("عرض ورق", "Sheet width", sheetWidthInput), fieldCell("طول ورق", "Sheet length", sheetHeightInput))
        kerfInput = field("عرض تیغه", "Saw kerf", "kerf", "۳")
        addTo(sheetCard, label("ضخامت تیغه برش (میلی‌متر)", "Saw kerf (mm)", 12f))
        addTo(sheetCard, kerfInput, 1)

        val cabinetCard = addSection(homePage, "مشخصات کابینت", "Cabinet specifications", "نوع کابینت و ابعاد بدنه", "Cabinet type and carcass dimensions")
        cabinetSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                if (english) arrayOf("Base cabinet", "Wall cabinet") else arrayOf("کابینت زمینی", "کابینت دیواری"))
            setSelection(prefs().getInt("cabinetType", 0).coerceIn(0, 1))
            background = rounded(Color.rgb(12, 37, 63), 12, Color.rgb(39, 105, 159), 1)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        addTo(cabinetCard, cabinetSpinner, 8)
        widthInput = field("عرض کابینت", "Cabinet width", "width", "۸۰۰")
        heightInput = field("ارتفاع کابینت", "Cabinet height", "height", "۷۲۰")
        depthInput = field("عمق کابینت", "Cabinet depth", "depth", "۵۶۰")
        thicknessInput = field("ضخامت ورق", "Board thickness", "thickness", "۱۶")
        shelfInput = field("تعداد طبقه", "Shelves", "shelves", "۱")
        doorInput = field("تعداد در", "Doors", "doors", "۲")
        plinthInput = field("ارتفاع پاخور", "Plinth height", "plinth", "۱۰۰")
        backThicknessInput = field("ضخامت پشت‌بند", "Back panel thickness", "backThickness", "۳")
        pairedRow(cabinetCard, fieldCell("عرض کابینت (mm)", "Cabinet width (mm)", widthInput), fieldCell("ارتفاع کابینت (mm)", "Cabinet height (mm)", heightInput))
        pairedRow(cabinetCard, fieldCell("عمق کابینت (mm)", "Cabinet depth (mm)", depthInput), fieldCell("ضخامت ورق بدنه", "Carcass thickness", thicknessInput))
        pairedRow(cabinetCard, fieldCell("تعداد طبقه", "Shelves", shelfInput), fieldCell("تعداد در (۰ تا ۲)", "Doors (0–2)", doorInput))
        pairedRow(cabinetCard, fieldCell("ارتفاع پاخور", "Plinth height", plinthInput), fieldCell("ضخامت پشت‌بند", "Back panel", backThicknessInput))

        val calculateButton = button("محاسبه و نمایش نقشه برش  →", "Calculate & view cutting plan  →") {
            saveSettings()
            calculate()
            showPage(1)
        }.apply {
            textSize = 16f
            minHeight = dp(58)
            background = AppTheme.primaryButton(this@MainActivity)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        addTo(homePage, calculateButton, 14)
        val hint = TextView(this).apply {
            text = tr("تنظیمات PVC و پشت‌بند در بخش «تنظیمات» قرار دارد.", "PVC and back-panel options are under Settings.")
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(10))
        }
        homePage.addView(hint)

        // RESULTS: independent page instead of being appended to the bottom of the input form.
        addPageHeading(resultsPage, "نتایج محاسبه", "Calculation results", "خلاصه، نقشه چیدمان و فهرست قطعات", "Summary, sheet layout and parts list")
        val metricsCard = addSection(resultsPage, "خلاصه برش", "Cutting summary")
        val metricsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        fun metric(value: String, fa: String, en: String): Pair<LinearLayout, TextView> {
            val valueText = TextView(this).apply { text = value; textSize = 21f; setTextColor(Color.WHITE); setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER }
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(12), dp(4), dp(12))
                background = rounded(Color.rgb(10, 39, 68), 13, Color.rgb(26, 108, 173), 1)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(3); marginEnd = dp(3) }
                addView(valueText)
                addView(TextView(this@MainActivity).apply { text = tr(fa, en); textSize = 10f; setTextColor(AppTheme.TEXT_SECONDARY); gravity = Gravity.CENTER; setPadding(0, dp(3), 0, 0) })
            }
            return box to valueText
        }
        val mSheets = metric("—", "تعداد ورق", "Sheets")
        val mWaste = metric("—", "درصد دورریز", "Waste")
        val mPieces = metric("—", "تعداد قطعات", "Pieces")
        metricSheets = mSheets.second
        metricWaste = mWaste.second
        metricPieces = mPieces.second
        metricsRow.addView(mSheets.first)
        metricsRow.addView(mWaste.first)
        metricsRow.addView(mPieces.first)
        addTo(metricsCard, metricsRow, 1)
        val layoutCard = addSection(resultsPage, "نقشه چیدمان ورق‌ها", "Sheet layout")
        layoutView = SheetLayoutView().apply { background = rounded(Color.rgb(4, 19, 34), 14, AppTheme.CARD_BORDER, 1) }
        layoutCard.addView(layoutView, LinearLayout.LayoutParams(-1, dp(270)))
        val partsCard = addSection(resultsPage, "فهرست قطعات و جزئیات", "Parts list & details")
        resultText = TextView(this).apply {
            text = tr("هنوز محاسبه‌ای انجام نشده است. به خانه برگردید و دکمه محاسبه را بزنید.", "No calculation yet. Return Home and tap Calculate.")
            textSize = 13f
            setTextColor(ink)
            setPadding(dp(13), dp(13), dp(13), dp(13))
            background = rounded(Color.rgb(5, 22, 39), 13, Color.rgb(25, 79, 124), 1)
        }
        addTo(partsCard, resultText, 1)
        addTo(resultsPage, button("ذخیره این پروژه", "Save this project") { saveProjectDialog() }, 8)

        // PROJECTS page is a dedicated destination, not a section far down the form.
        addPageHeading(projectsPage, "مدیریت پروژه‌ها", "Project manager", "طرح‌ها را ذخیره کنید، باز کنید یا نامشان را تغییر دهید", "Save, reopen or rename your designs")
        val projectCard = addSection(projectsPage, "پروژه‌های ذخیره‌شده", "Saved projects", "هر پروژه شامل ابعاد و تنظیمات فعلی است", "Each project stores dimensions and current options")
        addTo(projectCard, button("＋  ذخیره پروژه فعلی", "＋  Save current project") { saveSettings(); saveProjectDialog() }, 10)
        addTo(projectCard, button("↗  باز کردن پروژه", "↗  Open project") { chooseProjectToLoad() }, 10)
        addTo(projectCard, button("☷  مدیریت / تغییر نام / حذف", "☷  Manage / rename / delete") { manageProjectsDialog() }, 2)
        val projectNote = TextView(this).apply {
            text = tr("پروژه‌ها روی همین دستگاه ذخیره می‌شوند.", "Projects are stored locally on this device.")
            textSize = 12f; setTextColor(muted); setPadding(dp(3), dp(8), dp(3), dp(3))
        }
        projectCard.addView(projectNote)

        // SETTINGS page contains advanced toggles and language switch.
        addPageHeading(settingsPage, "تنظیمات", "Settings", "گزینه‌های محاسبه و زبان برنامه", "Calculation options and app language")
        val optionsCard = addSection(settingsPage, "گزینه‌های محاسبه", "Calculation options", "این گزینه‌ها روی نتیجه و برآورد PVC اثر می‌گذارند", "These options affect the estimate")
        includeBackCheck = check("محاسبه پشت‌بند", "Include back panel", "includeBack", true)
        legsCheck = check("کسر ارتفاع پاخور از بدنه زمینی", "Subtract plinth from base cabinet", "legs", true)
        doorPvcCheck = check("PVC چهار لبه درها", "PVC on all door edges", "doorPvc", true)
        sidePvcCheck = check("PVC لبه جلویی دو پهلو", "PVC on front edges of both sides", "sidePvc", true)
        shelfPvcCheck = check("PVC لبه جلویی کف و طبقه‌ها", "PVC on front edges of bottom and shelves", "shelfPvc", true)
        listOf(includeBackCheck, legsCheck, doorPvcCheck, sidePvcCheck, shelfPvcCheck).forEach { addTo(optionsCard, it, 5) }
        val languageCard = addSection(settingsPage, "زبان برنامه", "App language")
        addTo(languageCard, button(if (english) "تغییر به فارسی" else "Switch to English", if (english) "Switch to Persian" else "تغییر به انگلیسی") {
            saveSettings()
            english = !english
            prefs().edit().putBoolean("english", english).apply()
            buildScreen()
        }, 2)
        val aboutCard = addSection(settingsPage, "درباره MDF-KAR", "About MDF-KAR")
        aboutCard.addView(TextView(this).apply {
            text = tr("ابزار برآورد قطعات کابینت و چیدمان تقریبی برش ورق MDF. پیش از برش واقعی، اندازه‌ها را بررسی کنید.", "Cabinet part estimator and approximate MDF sheet cutting planner. Verify all measurements before cutting.")
            textSize = 12f; setTextColor(muted); setPadding(0, dp(5), 0, dp(3))
        })

        // Fixed four-item navigation. Each tab changes page; it never scrolls to a distant section.
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(7), dp(8), dp(7))
            background = AppTheme.navBackground(this@MainActivity)
            elevation = dp(12).toFloat()
        }
        fun navItem(icon: String, fa: String, en: String, index: Int) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(6), dp(3), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(2); marginEnd = dp(2) }
                isClickable = true
                isFocusable = true
                setOnClickListener { showPage(index) }
            }
            item.addView(TextView(this).apply { text = icon; textSize = 21f; gravity = Gravity.CENTER; setTextColor(muted) })
            item.addView(TextView(this).apply { text = tr(fa, en); textSize = 10f; gravity = Gravity.CENTER; setTextColor(muted); setPadding(0, dp(2), 0, 0) })
            nav.addView(item)
        }
        navItem("⌂", "خانه", "Home", 0)
        navItem("▤", "نتایج", "Results", 1)
        navItem("▣", "پروژه‌ها", "Projects", 2)
        navItem("⚙", "تنظیمات", "Settings", 3)
        root.addView(nav, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
        showPage(0)
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
        metricSheets.text = if (english) sheetCount.toString() else faDigits(sheetCount.toString())
        val unusedArea = max(0.0, sheetCount * sheetArea - boardArea)
        val wastePercent = if (sheetCount > 0) unusedArea / (sheetCount * sheetArea) * 100.0 else 0.0
        metricWaste.text = if (english) "${fmt(wastePercent)}%" else faDigits("${fmt(wastePercent)}٪")
        metricPieces.text = if (english) layout.placements.size.toString() else faDigits(layout.placements.size.toString())
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
