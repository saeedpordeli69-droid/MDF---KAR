
package ir.mdfkar.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil

class MainActivity : Activity() {

    private var english = false
    private var cabinetTypeIndex = 0

    private lateinit var widthInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var depthInput: EditText
    private lateinit var thicknessInput: EditText
    private lateinit var shelfCountInput: EditText
    private lateinit var doorCountInput: EditText
    private lateinit var plinthInput: EditText
    private lateinit var backThicknessInput: EditText

    private lateinit var cabinetSpinner: Spinner
    private lateinit var backCheck: CheckBox
    private lateinit var legsCheck: CheckBox
    private lateinit var pvcDoorsCheck: CheckBox
    private lateinit var pvcSidesCheck: CheckBox
    private lateinit var pvcHorizontalCheck: CheckBox
    private lateinit var resultText: TextView

    private val green = Color.rgb(20, 125, 114)
    private val dark = Color.rgb(18, 60, 58)
    private val pale = Color.rgb(241, 247, 245)
    private val white = Color.WHITE

    private val values = mutableMapOf<String, String>()

    private val prefs by lazy {
        getSharedPreferences("mdf_kar_project", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listOf(
            "width", "height", "depth", "thickness",
            "shelves", "doors", "plinth", "backThickness"
        ).forEach { key ->
            val default = when (key) {
                "thickness" -> "16"
                "shelves" -> "1"
                "doors" -> "0"
                "plinth" -> "100"
                "backThickness" -> "3"
                else -> ""
            }
            values[key] = prefs.getString(key, default) ?: default
        }

        english = prefs.getBoolean("english", false)
        cabinetTypeIndex = prefs.getInt("cabinetType", 0)

        buildScreen()
    }

    private fun faDigits(value: String): String {
        val en = "0123456789"
        val fa = "۰۱۲۳۴۵۶۷۸۹"

        return value.map { ch ->
            val i = en.indexOf(ch)
            if (i >= 0) fa[i]
            else if (ch == '.') '٫'
            else ch
        }.joinToString("")
    }

    private fun number(text: String): Double? {
        val normalized = text
            .replace('۰', '0').replace('۱', '1')
            .replace('۲', '2').replace('۳', '3')
            .replace('۴', '4').replace('۵', '5')
            .replace('۶', '6').replace('۷', '7')
            .replace('۸', '8').replace('۹', '9')
            .replace('٫', '.').replace(',', '.')
            .replace('٠', '0').replace('١', '1')
            .replace('٢', '2').replace('٣', '3')
            .replace('٤', '4').replace('٥', '5')
            .replace('٦', '6').replace('٧', '7')
            .replace('٨', '8').replace('٩', '9')

        return normalized.trim().toDoubleOrNull()
    }

    private fun fmt(value: Double): String {
        return DecimalFormat(
            "0.##",
            DecimalFormatSymbols(Locale.US)
        ).format(value)
    }

    private fun display(value: String): String {
        return if (english) value else faDigits(value)
    }

    private fun label(
        fa: String,
        en: String,
        size: Float = 15f
    ): TextView {
        return TextView(this).apply {
            text = if (english) en else fa
            textSize = size
            setTextColor(dark)
            gravity = if (english) Gravity.START else Gravity.END
            layoutDirection = if (english) {
                View.LAYOUT_DIRECTION_LTR
            } else {
                View.LAYOUT_DIRECTION_RTL
            }
        }
    }

    private fun field(
        key: String,
        hintFa: String,
        hintEn: String,
        defaultValue: String = ""
    ): EditText {
        return EditText(this).apply {
            hint = if (english) hintEn else hintFa
            textSize = 16f
            setSingleLine(true)
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            setPadding(18, 8, 18, 8)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setBackgroundColor(white)
            setText(values[key] ?: defaultValue)
        }
    }

    private fun actionButton(
        fa: String,
        en: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = if (english) en else fa
            isAllCaps = false
            setTextColor(white)
            setBackgroundColor(green)
            setOnClickListener { action() }
        }
    }

    private fun check(
        fa: String,
        en: String,
        checked: Boolean
    ): CheckBox {
        return CheckBox(this).apply {
            text = if (english) en else fa
            isChecked = checked
            setTextColor(dark)
        }
    }

    private fun collectValues() {
        values["width"] = widthInput.text.toString()
        values["height"] = heightInput.text.toString()
        values["depth"] = depthInput.text.toString()
        values["thickness"] = thicknessInput.text.toString()
        values["shelves"] = shelfCountInput.text.toString()
        values["doors"] = doorCountInput.text.toString()
        values["plinth"] = plinthInput.text.toString()
        values["backThickness"] = backThicknessInput.text.toString()
        cabinetTypeIndex = cabinetSpinner.selectedItemPosition
    }

    private fun saveProject() {
        collectValues()

        prefs.edit()
            .putString("width", values["width"])
            .putString("height", values["height"])
            .putString("depth", values["depth"])
            .putString("thickness", values["thickness"])
            .putString("shelves", values["shelves"])
            .putString("doors", values["doors"])
            .putString("plinth", values["plinth"])
            .putString("backThickness", values["backThickness"])
            .putBoolean("english", english)
            .putInt("cabinetType", cabinetTypeIndex)
            .putBoolean("includeBack", backCheck.isChecked)
            .putBoolean("includeLegs", legsCheck.isChecked)
            .putBoolean("pvcDoors", pvcDoorsCheck.isChecked)
            .putBoolean("pvcSides", pvcSidesCheck.isChecked)
            .putBoolean("pvcHorizontal", pvcHorizontalCheck.isChecked)
            .apply()

        Toast.makeText(
            this,
            if (english) "Project saved on this device."
            else "اطلاعات پروژه روی این دستگاه ذخیره شد.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pale)
            layoutDirection = if (english) {
                View.LAYOUT_DIRECTION_LTR
            } else {
                View.LAYOUT_DIRECTION_RTL
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 10, 16, 10)
            setBackgroundColor(dark)
        }

        val title = TextView(this).apply {
            text = if (english) "MDF Kar" else "ام‌دی‌اف‌کار"
            textSize = 23f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        val languageButton = Button(this).apply {
            text = if (english) "فارسی" else "English"
            isAllCaps = false
            setOnClickListener {
                collectValues()
                english = !english
                prefs.edit().putBoolean("english", english).apply()
                buildScreen()
            }
        }

        header.addView(title)
        header.addView(languageButton)
        root.addView(header)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 24)
        }

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(
            label("محاسبه کابینت و لیست برش", "Cabinet & Cutting List", 20f)
                .apply { typeface = Typeface.DEFAULT_BOLD }
        )

        content.addView(
            label(
                "همه ابعاد بر حسب میلی‌متر هستند.",
                "All dimensions are in millimeters."
            ).apply { setPadding(0, 8, 0, 12) }
        )

        val spinnerLabel = label("نوع کابینت", "Cabinet type")
        content.addView(spinnerLabel)

        cabinetSpinner = Spinner(this)
        val cabinetOptions = if (english) {
            listOf("Base cabinet", "Wall cabinet")
        } else {
            listOf("کابینت زمینی", "کابینت دیواری")
        }

        cabinetSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            cabinetOptions
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        cabinetSpinner.setSelection(cabinetTypeIndex.coerceIn(0, 1))
        content.addView(cabinetSpinner)

        widthInput = field("width", "عرض کابینت", "Cabinet width")
        heightInput = field("height", "ارتفاع کل بدنه", "Overall cabinet height")
        depthInput = field("depth", "عمق کابینت", "Cabinet depth")
        thicknessInput = field(
            "thickness",
            "ضخامت ورق بدنه (پیش‌فرض ۱۶)",
            "Carcass board thickness (default 16)",
            "16"
        )
        shelfCountInput = field(
            "shelves",
            "تعداد طبقات داخلی",
            "Number of internal shelves",
            "1"
        )
        doorCountInput = field(
            "doors",
            "تعداد درب (۰ تا ۲)",
            "Door count (0–2)",
            "0"
        )
        plinthInput = field(
            "plinth",
            "ارتفاع پاخور زمینی (پیش‌فرض ۱۰۰)",
            "Base plinth height (default 100)",
            "100"
        )
        backThicknessInput = field(
            "backThickness",
            "ضخامت پشت کابینت (پیش‌فرض ۳)",
            "Back panel thickness (default 3)",
            "3"
        )

        val fields = listOf(
            widthInput, heightInput, depthInput, thicknessInput,
            shelfCountInput, doorCountInput, plinthInput, backThicknessInput
        )

        fields.forEach { input ->
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.topMargin = 8
            content.addView(input, lp)
        }

        content.addView(
            label(
                "گزینه‌های ساخت",
                "Construction options",
                18f
            ).apply {
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 16, 0, 4)
            }
        )

        backCheck = check(
            "افزودن پشت کابینت",
            "Include back panel",
            prefs.getBoolean("includeBack", false)
        )

        legsCheck = check(
            "محاسبه پایه و پاخور زمینی",
            "Include base legs and plinth",
            prefs.getBoolean("includeLegs", true)
        )

        content.addView(backCheck)
        content.addView(legsCheck)

        content.addView(
            label(
                "لبه‌های PVC",
                "PVC edge selection",
                18f
            ).apply {
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 16, 0, 4)
            }
        )

        pvcDoorsCheck = check(
            "PVC دور چهار طرف درب‌ها",
            "PVC on all four door edges",
            prefs.getBoolean("pvcDoors", true)
        )

        pvcSidesCheck = check(
            "PVC لبه جلویی پهلوها",
            "PVC on front edges of side panels",
            prefs.getBoolean("pvcSides", true)
        )

        pvcHorizontalCheck = check(
            "PVC لبه جلویی کف و طبقات",
            "PVC on front edges of bottom and shelves",
            prefs.getBoolean("pvcHorizontal", true)
        )

        content.addView(pvcDoorsCheck)
        content.addView(pvcSidesCheck)
        content.addView(pvcHorizontalCheck)

        content.addView(
            label(
                "ابعاد ورق مبنا برای برآورد: ۲۸۰۰ × ۲۰۷۰ میلی‌متر.",
                "Reference sheet size for estimate: 2800 × 2070 mm."
            ).apply { setPadding(0, 12, 0, 8) }
        )

        resultText = label(
            "ابعاد را وارد کن و محاسبه را بزن.",
            "Enter dimensions and calculate."
        )
        resultText.textSize = 14f
        resultText.setPadding(0, 16, 0, 16)

        content.addView(
            actionButton("محاسبه لیست برش", "Calculate cutting list") {
                calculate()
            }
        )

        content.addView(
            actionButton("ذخیره پروژه", "Save project") {
                saveProject()
            }.apply {
                setBackgroundColor(dark)
            }
        )

        content.addView(resultText)

        setContentView(root)
    }

    private fun calculate() {
        collectValues()

        val w = number(values["width"] ?: "")
        val h = number(values["height"] ?: "")
        val d = number(values["depth"] ?: "")
        val t = number(values["thickness"] ?: "16")
        val shelvesValue = number(values["shelves"] ?: "1")
        val doorsValue = number(values["doors"] ?: "0")
        val plinthValue = number(values["plinth"] ?: "100")
        val backT = number(values["backThickness"] ?: "3")

        if (
            w == null || h == null || d == null || t == null ||
            shelvesValue == null || doorsValue == null ||
            plinthValue == null || backT == null ||
            w <= 0 || h <= 0 || d <= 0 || t <= 0 ||
            shelvesValue < 0 || shelvesValue > 20 ||
            shelvesValue % 1.0 != 0.0 ||
            doorsValue < 0 || doorsValue > 2 ||
            doorsValue % 1.0 != 0.0 ||
            plinthValue < 0 || backT <= 0
        ) {
            resultText.text = if (english) {
                "Please check dimensions, thickness, shelf count, door count and plinth height."
            } else {
                "ابعاد، ضخامت، تعداد طبقات و درب و ارتفاع پاخور را بررسی کن."
            }
            return
        }

        val isBase = cabinetSpinner.selectedItemPosition == 0
        val useLegs = isBase && legsCheck.isChecked
        val plinth = if (useLegs) plinthValue else 0.0
        val bodyHeight = h - plinth
        val innerWidth = w - 2 * t
        val doorCount = doorsValue.toInt()
        val shelfCount = shelvesValue.toInt()

        if (
            bodyHeight <= 2 * t ||
            innerWidth <= 0 ||
            d <= t ||
            (backCheck.isChecked && backT >= t)
        ) {
            resultText.text = if (english) {
                "Dimensions are incompatible with the selected board thickness or plinth."
            } else {
                "ابعاد با ضخامت ورق یا ارتفاع پاخور سازگار نیست."
            }
            return
        }

        val parts = mutableListOf<String>()
        var carcassArea = 0.0
        var backArea = 0.0
        var pvcLengthMm = 0.0

        fun addPart(
            fa: String,
            en: String,
            count: Int,
            a: Double,
            b: Double,
            boardThickness: Double = t,
            areaGroup: String = "carcass"
        ) {
            if (count <= 0) return

            val line = if (english) {
                "$en: $count pcs — ${fmt(a)} × ${fmt(b)} × ${fmt(boardThickness)} mm"
            } else {
                "$fa: ${faDigits(count.toString())} عدد — " +
                    "${faDigits(fmt(a))} × ${faDigits(fmt(b))} × " +
                    "${faDigits(fmt(boardThickness))} میلی‌متر"
            }

            parts.add(line)

            val area = count * a * b / 1_000_000.0

            if (areaGroup == "back") {
                backArea += area
            } else {
                carcassArea += area
            }
        }

        addPart("پهلوهای بدنه", "Side panels", 2, bodyHeight, d)

        addPart("کف کابینت", "Bottom panel", 1, innerWidth, d)

        addPart(
            "طبقات داخلی",
            "Internal shelves",
            shelfCount,
            innerWidth,
            d
        )

        addPart(
            "کلاف‌های بالایی",
            "Top rails",
            2,
            innerWidth,
            100.0
        )

        if (backCheck.isChecked) {
            addPart(
                "پشت کابینت",
                "Back panel",
                1,
                w,
                bodyHeight,
                backT,
                "back"
            )
        }

        val doorHeight = bodyHeight - 3.0

        if (doorCount == 1 && doorHeight > 0) {
            addPart("درب", "Door", 1, w - 3.0, doorHeight)
        } else if (doorCount == 2 && doorHeight > 0) {
            addPart(
                "درب‌های مساوی",
                "Equal doors",
                2,
                (w - 4.0) / 2.0,
                doorHeight
            )
        }

        if (useLegs) {
            val legsCount = 4
            parts.add(
                if (english) {
                    "Cabinet legs: $legsCount pcs (hardware; not included in board area)"
                } else {
                    "پایه کابینت: ${faDigits(legsCount.toString())} عدد (یراق؛ جزو مساحت ورق نیست)"
                }
            )

            parts.add(
                if (english) {
                    "Plinth: height ${fmt(plinth)} mm; length and sections depend on installation."
                } else {
                    "پاخور: ارتفاع ${faDigits(fmt(plinth))} میلی‌متر؛ طول و قطعات به روش نصب بستگی دارد."
                }
            )
        }

        if (pvcDoorsCheck.isChecked && doorCount > 0 && doorHeight > 0) {
            val doorWidth = when (doorCount) {
                1 -> w - 3.0
                2 -> (w - 4.0) / 2.0
                else -> 0.0
            }
            pvcLengthMm += doorCount * 2.0 * (doorWidth + doorHeight)
        }

        if (pvcSidesCheck.isChecked) {
            pvcLengthMm += 2.0 * bodyHeight
        }

        if (pvcHorizontalCheck.isChecked) {
            pvcLengthMm += (1 + shelfCount) * innerWidth
        }

        val sheetArea = 2.8 * 2.07
        val carcassSheets = ceil(carcassArea / sheetArea).toInt()
        val backSheets = if (backArea > 0) {
            ceil(backArea / sheetArea).toInt()
        } else {
            0
        }

        val estimatedWaste = if (carcassSheets > 0) {
            (carcassSheets * sheetArea - carcassArea) /
                (carcassSheets * sheetArea) * 100.0
        } else {
            0.0
        }

        val output = buildString {
            append(
                if (english) {
                    "PRELIMINARY CUTTING LIST\n\n"
                } else {
                    "لیست برش اولیه\n\n"
                }
            )

            append(parts.joinToString("\n"))
            append("\n\n")

            append(
                if (english) {
                    "Cabinet type: ${if (isBase) "Base" else "Wall"}\n"
                } else {
                    "نوع کابینت: ${if (isBase) "زمینی" else "دیواری"}\n"
                }
            )

            append(
                if (english) {
                    "Carcass board area: ${fmt(carcassArea)} m²\n" +
                        "Estimated 2800 × 2070 sheets: $carcassSheets\n" +
                        "Area-based unused percentage: ${fmt(estimatedWaste)}%\n"
                } else {
                    "مساحت قطعات ورق بدنه و درب: ${faDigits(fmt(carcassArea))} مترمربع\n" +
                        "تعداد ورق تخمینی ۲۸۰۰ × ۲۰۷۰: ${faDigits(carcassSheets.toString())}\n" +
                        "درصد سطح استفاده‌نشده بر اساس مساحت: ${faDigits(fmt(estimatedWaste))}٪\n"
                }
            )

            if (backCheck.isChecked) {
                append(
                    if (english) {
                        "Back panel area: ${fmt(backArea)} m²; estimated sheets: $backSheets\n"
                    } else {
                        "مساحت پشت کابینت: ${faDigits(fmt(backArea))} مترمربع؛ " +
                            "تعداد ورق تخمینی: ${faDigits(backSheets.toString())}\n"
                    }
                )
            }

            append(
                if (english) {
                    "Estimated selected PVC length: ${fmt(pvcLengthMm / 1000.0)} m\n\n"
                } else {
                    "طول تقریبی PVC برای لبه‌های انتخاب‌شده: " +
                        "${faDigits(fmt(pvcLengthMm / 1000.0))} متر\n\n"
                }
            )

            append(
                if (english) {
                    "IMPORTANT: Sheet counts and waste are area estimates, not an optimized cutting layout. Door clearances, back grooves, edge banding thickness, saw kerf, joints, hardware and installation details must be checked before cutting."
                } else {
                    "توجه: تعداد ورق و پرت بر اساس مساحت تخمین زده شده و نقشه چیدمان بهینه نیست. " +
                        "فاصله واقعی درب‌ها، شیار پشت، ضخامت PVC، عرض تیغه، اتصالات و یراق باید " +
                        "قبل از برش بررسی شوند."
                }
            )
        }

        resultText.text = output
    }
}
