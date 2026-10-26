package ir.mdfkar.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DecimalFormat
import java.util.Locale

class MainActivity : Activity() {
    private var english = false
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var languageButton: Button
    private lateinit var widthInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var depthInput: EditText
    private lateinit var thicknessInput: EditText
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
            val idx = en.indexOf(ch)
            if (idx >= 0) fa[idx] else if (ch == '.') '٫' else ch
        }.joinToString("")
    }

    private fun number(text: String): Double? {
        val normalized = text
            .replace('۰', '0').replace('۱', '1').replace('۲', '2')
            .replace('۳', '3').replace('۴', '4').replace('۵', '5')
            .replace('۶', '6').replace('۷', '7').replace('۸', '8')
            .replace('۹', '9').replace('٫', '.').replace(',', '.')
            .replace('٠', '0').replace('١', '1').replace('٢', '2')
            .replace('٣', '3').replace('٤', '4').replace('٥', '5')
            .replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
        return normalized.toDoubleOrNull()
    }

    private fun label(textFa: String, textEn: String, size: Float = 15f): TextView {
        return TextView(this).apply {
            text = if (english) textEn else textFa
            textSize = size
            setTextColor(dark)
            gravity = if (english) Gravity.START else Gravity.END
            layoutDirection = if (english) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        }
    }

    private fun field(hintFa: String, hintEn: String): EditText {
        return EditText(this).apply {
            hint = if (english) hintEn else hintFa
            textSize = 16f
            singleLine = true
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            setPadding(18, 8, 18, 8)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setBackgroundColor(Color.WHITE)
        }
    }

    private fun button(textFa: String, textEn: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = if (english) textEn else textFa
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener { action() }
        }
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
        languageButton = Button(this).apply {
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
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 24)
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(label("نسخه اولیه برنامه نجاری و کابینت‌سازی", "Woodworking & Cabinet App — Starter", 18f))
        content.addView(label("ابعاد پروژه (میلی‌متر)", "Project dimensions (millimeters)").apply {
            setPadding(0, 18, 0, 8)
        })

        widthInput = field("عرض", "Width")
        heightInput = field("ارتفاع", "Height")
        depthInput = field("عمق", "Depth")
        thicknessInput = field("ضخامت ورق (مثلاً ۱۶)", "Board thickness (e.g. 16)")
        listOf(widthInput, heightInput, depthInput, thicknessInput).forEach {
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.bottomMargin = 10
            content.addView(it, lp)
        }
        resultText = label("ابعاد را وارد کنید و محاسبه را بزنید.", "Enter dimensions and calculate.")
        resultText.setPadding(0, 12, 0, 12)
        content.addView(button("محاسبه اولیه", "Calculate draft") { calculate() })
        content.addView(resultText)

        content.addView(label("بخش‌های برنامه", "App modules").apply {
            setPadding(0, 20, 0, 8)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        })
        val modules = listOf(
            "آنالیز قطعات" to "Parts analysis",
            "نقشه برش کات‌مستر" to "Cutting layout",
            "تعیین لبه‌های PVC" to "PVC edge mapping",
            "تعداد ورق و میزان پرت" to "Board count and waste",
            "کشو و محاسبات کابینت" to "Drawer and cabinet sizing",
            "برگه چاپی خدمات برش / PDF" to "Printable cutting sheet / PDF",
            "یراق‌آلات و قیمت‌گذاری" to "Hardware and costing",
            "سفارش‌ها و حساب مشتری" to "Orders and customer ledger"
        )
        modules.forEach { (fa, en) ->
            val b = Button(this).apply {
                text = "•  " + if (english) en else fa
                isAllCaps = false
                gravity = if (english) Gravity.START or Gravity.CENTER_VERTICAL else Gravity.END or Gravity.CENTER_VERTICAL
                setTextColor(dark)
                setBackgroundColor(Color.WHITE)
                setOnClickListener {
                    Toast.makeText(this@MainActivity,
                        if (english) "This module will be added in the next development steps." else "این بخش در مراحل بعدی توسعه تکمیل می‌شود.",
                        Toast.LENGTH_SHORT).show()
                }
            }
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.bottomMargin = 7
            content.addView(b, lp)
        }
        setContentView(root)
    }

    private fun calculate() {
        val w = number(widthInput.text.toString())
        val h = number(heightInput.text.toString())
        val d = number(depthInput.text.toString())
        val t = number(thicknessInput.text.toString())
        if (w == null || h == null || d == null || t == null ||
            w <= 0 || h <= 0 || d <= 0 || t <= 0) {
            resultText.text = if (english) "Please enter valid positive dimensions." else "لطفاً همه ابعاد را به‌درستی وارد کنید."
            return
        }
        val area = (w * h + 2 * h * d + 2 * w * d) / 1_000_000.0
        val fmt = DecimalFormat("0.##", java.text.DecimalFormatSymbols(Locale.US))
        val raw = if (english) {
            "Dimensions: ${fmt.format(w)} × ${fmt.format(h)} × ${fmt.format(d)} mm\\nBoard thickness: ${fmt.format(t)} mm\\nApproximate surface area: ${fmt.format(area)} m²\\nNote: this is a preliminary estimate, not a cutting list."
        } else {
            "ابعاد: ${fmt.format(w)} × ${fmt.format(h)} × ${fmt.format(d)} میلی‌متر\\nضخامت ورق: ${fmt.format(t)} میلی‌متر\\nمساحت تقریبی سطوح: ${fmt.format(area)} مترمربع\\nتوجه: این فقط برآورد اولیه است و هنوز لیست برش اجرایی نیست."
        }
        resultText.text = if (english) raw else faDigits(raw)
    }
}
