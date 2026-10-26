
package ir.mdfkar.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class MainActivity : Activity() {

    private var english = false

    private lateinit var widthInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var depthInput: EditText
    private lateinit var thicknessInput: EditText
    private lateinit var doorCountInput: EditText
    private lateinit var backCheck: CheckBox
    private lateinit var resultText: TextView

    private val green = Color.rgb(20, 125, 114)
    private val dark = Color.rgb(18, 60, 58)
    private val pale = Color.rgb(241, 247, 245)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        return normalized.toDoubleOrNull()
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
            layoutDirection = if (english)
                View.LAYOUT_DIRECTION_LTR
            else
                View.LAYOUT_DIRECTION_RTL
        }
    }

    private fun field(
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
            setBackgroundColor(Color.WHITE)
            if (defaultValue.isNotEmpty()) setText(defaultValue)
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
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener { action() }
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pale)
            layoutDirection = if (english)
                View.LAYOUT_DIRECTION_LTR
            else
                View.LAYOUT_DIRECTION_RTL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(18, 12, 18, 12)
            setBackgroundColor(dark)
        }

        val title = TextView(this).apply {
            text = if (english) "MDF Kar" else "ام‌دی‌اف‌کار"
            textSize = 23f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        val languageButton = Button(this).apply {
            text = if (english) "فارسی" else "English"
            isAllCaps = false
            setOnClickListener {
                english = !english
                buildScreen()
            }
        }

        header.addView(title)
        header.addView(languageButton)
        root.addView(header)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 24)
        }

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(
            label(
                "محاسبه کابینت زمینی",
                "Base cabinet calculator",
                20f
            ).apply {
                typeface = Typeface.DEFAULT_BOLD
            }
        )

        content.addView(
            label(
                "تمام اندازه‌ها برحسب میلی‌متر هستند.",
                "All dimensions are in millimeters."
            ).apply {
                setPadding(0, 8, 0, 14)
            }
        )

        widthInput = field("عرض کابینت", "Cabinet width")
        heightInput = field("ارتفاع بدنه کابینت", "Cabinet body height")
        depthInput = field("عمق کابینت", "Cabinet depth")
        thicknessInput = field(
            "ضخامت ورق بدنه",
            "Carcass board thickness",
            "16"
        )
        doorCountInput = field(
            "تعداد درب: صفر، یک یا دو",
            "Door count: 0, 1 or 2",
            "0"
        )

        listOf(
            widthInput,
            heightInput,
            depthInput,
            thicknessInput,
            doorCountInput
        ).forEach { input ->
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.bottomMargin = 10
            content.addView(input, lp)
        }

        backCheck = CheckBox(this).apply {
            text = if (english)
                "Include 3 mm back panel"
            else
                "محاسبه پشت کابینت با ورق ۳ میلی‌متر"
            isChecked = false
            setTextColor(dark)
        }
        content.addView(backCheck)

        content.addView(
            label(
                "فرض این نسخه: یک طبقه داخلی و دو کلاف بالایی ۱۰ سانتی‌متری.",
                "Assumption: one shelf and two 100 mm top rails."
            ).apply {
                setPadding(0, 8, 0, 12)
            }
        )

        resultText = label(
            "ابعاد را وارد کن و محاسبه را بزن.",
            "Enter dimensions and calculate."
        )
        resultText.setPadding(0, 14, 0, 14)

        content.addView(
            actionButton("محاسبه لیست برش", "Calculate cutting list") {
                calculate()
            }
        )
        content.addView(resultText)

        content.addView(
            label(
                "مراحل بعدی توسعه",
                "Upcoming modules",
                18f
            ).apply {
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 18, 0, 8)
            }
        )

        val modules = listOf(
            "تعداد طبقات قابل تنظیم" to "Adjustable shelf count",
            "پایه و پاخور کابینت" to "Legs and plinth",
            "لبه‌های PVC" to "PVC edge mapping",
            "بهینه‌سازی چیدمان ورق" to "Board cutting optimization",
            "خروجی PDF خدمات برش" to "Cutting sheet PDF"
        )

        modules.forEach { (fa, en) ->
            val b = Button(this).apply {
                text = if (english) en else fa
                isAllCaps = false
                setTextColor(dark)
                setBackgroundColor(Color.WHITE)
                setOnClickListener {
                    Toast.makeText(
                        this@MainActivity,
                        if (english)
                            "This feature is planned for the next development stage."
                        else
                            "این قابلیت در مرحله بعد اضافه می‌شود.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.bottomMargin = 6
            content.addView(b, lp)
        }

        setContentView(root)
    }

    private fun calculate() {
        val w = number(widthInput.text.toString())
        val h = number(heightInput.text.toString())
        val d = number(depthInput.text.toString())
        val t = number(thicknessInput.text.toString())
        val doorsValue = number(doorCountInput.text.toString())

        if (
            w == null || h == null || d == null || t == null ||
            doorsValue == null ||
            w <= 0 || h <= 0 || d <= 0 || t <= 0 ||
            w <= 2 * t || doorsValue % 1.0 != 0.0 ||
            doorsValue < 0 || doorsValue > 2
        ) {
            resultText.text = if (english)
                "Check all dimensions, board thickness and door count (0–2)."
            else
                "ابعاد و ضخامت را بررسی کن. تعداد درب باید صفر، یک یا دو باشد."
            return
        }

        val innerWidth = w - 2 * t
        val doorCount = doorsValue.toInt()
        val fmt = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))
        val pieces = mutableListOf<String>()

        fun addPiece(fa: String, en: String, count: Int, a: Double, b: Double) {
            if (english) {
                pieces.add("$en: $count pcs — ${fmt.format(a)} × ${fmt.format(b)}")
            } else {
                pieces.add("$fa: $count عدد — ${faDigits(fmt.format(a))} × ${faDigits(fmt.format(b))}")
            }
        }

        addPiece("پهلوهای بدنه", "Side panels", 2, h, d)
        addPiece("کف کابینت", "Bottom panel", 1, innerWidth, d)
        addPiece("طبقه داخلی", "Shelf", 1, innerWidth, d)
        addPiece("کلاف‌های بالایی", "Top rails", 2, innerWidth, 100.0)

        if (backCheck.isChecked) {
            addPiece("پشت کابینت (ورق ۳ میلی‌متر)", "Back panel (3 mm)", 1, w, h)
        }

        if (doorCount == 1) {
            addPiece("درب", "Door", 1, w - 3, h - 3)
        } else if (doorCount == 2) {
            addPiece("درب‌های مساوی", "Equal doors", 2, (w - 4) / 2, h - 3)
        }

        val text = buildString {
            append(
                if (english) "PRELIMINARY CUTTING LIST\n\n"
                else "لیست برش اولیه کابینت\n\n"
            )
            append(pieces.joinToString("\n"))
            append("\n\n")
            append(
                if (english) "Carcass board thickness: ${fmt.format(t)} mm"
                else "ضخامت ورق بدنه: ${faDigits(fmt.format(t))} میلی‌متر"
            )
            append("\n\n")
            append(
                if (english)
                    "Assumptions: one shelf, two 100 mm rails. Door sizes assume 3 mm total clearance for one door, or 4 mm total width clearance for two doors. Hinges, edge banding, back grooves and installation details are not included. Verify before cutting."
                else
                    "فرض‌ها: یک طبقه و دو کلاف ۱۰۰ میلی‌متری. اندازه درب‌ها با فرض فاصله نصب تقریبی محاسبه شده است. لولا، PVC، شیار پشت و جزئیات نصب لحاظ نشده‌اند؛ قبل از برش اندازه‌ها را بررسی کن."
            )
        }

        resultText.text = text
    }
}
