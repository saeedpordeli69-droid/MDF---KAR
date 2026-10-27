package ir.mdfkar.app

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
import android.text.InputType
import java.io.File
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal fun MainActivity.buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(3, 13, 27))
            layoutDirection = if (english) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
            // Insets are applied below after the view is attached, so the app navigation
            // never sits underneath Android's gesture/three-button navigation bar.
            setPadding(0, 0, 0, 0)
        }

        val pageHost = android.widget.FrameLayout(this)
        val pages = mutableListOf<LinearLayout>()
        fun makePage(): LinearLayout {
            val pageContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(28), dp(16), dp(28))
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
        // Keep the hero close to the status bar while leaving a safe visual gap below system icons.
        homePage.setPadding(dp(16), dp(28), dp(16), dp(28))
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
            val stepper = LinearLayout(this@buildScreen).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(Color.rgb(10, 31, 53), 13, Color.rgb(43, 102, 153), 1)
                setPadding(dp(4), dp(2), dp(4), dp(2))
            }
            fun stepButton(symbol: String, delta: Double): TextView = TextView(this@buildScreen).apply {
                text = symbol
                textSize = 19f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = rounded(Color.rgb(13, 68, 124), 50, Color.rgb(27, 113, 190), 1)
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val current = number(input.text.toString()) ?: 0.0
                    val next = (current + delta).coerceAtLeast(0.0)
                    val value = if (next % 1.0 == 0.0) next.toLong().toString() else fmt(next)
                    input.setText(if (english) value else faDigits(value))
                    input.setSelection(input.text.length)
                }
            }
            stepper.addView(stepButton("−", -1.0))
            stepper.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
            stepper.addView(stepButton("+", 1.0))
            addView(stepper)
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
                setPadding(dp(16), dp(12), dp(16), dp(12))
                background = AppTheme.heroBackground(this@buildScreen)
                elevation = dp(3).toFloat()
            }
            header.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val mark = TextView(this@buildScreen).apply {
                    text = "▰"
                    textSize = 25f
                    setTextColor(AppTheme.ACCENT)
                    setPadding(0, 0, dp(7), 0)
                }
                addView(mark)
                val brand = TextView(this@buildScreen).apply {
                    text = android.text.SpannableString("MDF-KAR").apply {
                        setSpan(android.text.style.ForegroundColorSpan(Color.WHITE), 0, 3, 0)
                        setSpan(android.text.style.ForegroundColorSpan(AppTheme.ACCENT), 4, 7, 0)
                    }
                    textSize = 25f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    letterSpacing = 0.025f
                }
                addView(brand)
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
            addTo(target, header, 10)
        }

        // HOME: reference hero artwork includes the centered white/blue logo, MDF boards and slogan.
        val homeHero = ImageView(this).apply {
            setImageResource(ir.mdfkar.app.R.drawable.mdf_hero)
            scaleType = android.widget.ImageView.ScaleType.FIT_XY
            contentDescription = tr("لوگوی MDF-KAR و ورق‌های MDF؛ برش هوشمند، صرفه‌جویی در زمان و هزینه", "MDF-KAR logo and MDF boards; smart cutting saves time and cost")
            adjustViewBounds = false
        }
        homePage.addView(homeHero, LinearLayout.LayoutParams(-1, dp(164)).apply { bottomMargin = dp(10) })
        val sheetCard = addSection(homePage, "ابعاد ورق MDF", "MDF sheet dimensions", "همه اندازه‌ها بر حسب میلی‌متر", "All measurements in millimeters")
        sheetWidthInput = field("عرض ورق", "Sheet width", "sheetWidth", "۲۰۷۰")
        sheetHeightInput = field("طول ورق", "Sheet length", "sheetHeight", "۲۸۰۰")
        pairedRow(sheetCard, fieldCell("عرض ورق", "Sheet width", sheetWidthInput), fieldCell("طول ورق", "Sheet length", sheetHeightInput))
        kerfInput = field("عرض تیغه", "Saw kerf", "kerf", "۳")
        addTo(sheetCard, label("ضخامت تیغه برش (میلی‌متر)", "Saw kerf (mm)", 12f))
        addTo(sheetCard, kerfInput, 1)

        val cabinetCard = addSection(homePage, "مشخصات کابینت", "Cabinet specifications", "نوع کابینت و ابعاد بدنه", "Cabinet type and carcass dimensions")
        cabinetSpinner = Spinner(this).apply {
            val cabinetTypes = if (english) arrayOf("Base cabinet", "Wall cabinet") else arrayOf("کابینت زمینی", "کابینت دیواری")
            adapter = object : ArrayAdapter<String>(this@buildScreen, android.R.layout.simple_spinner_item, cabinetTypes) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getView(position, convertView, parent) as TextView).apply {
                        setTextColor(AppTheme.TEXT)
                        textSize = 15f
                        setPadding(dp(10), dp(8), dp(10), dp(8))
                    }
                }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                        setTextColor(AppTheme.TEXT)
                        textSize = 15f
                        setBackgroundColor(Color.rgb(7, 24, 43))
                        setPadding(dp(16), dp(12), dp(16), dp(12))
                    }
                }
            }.also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(prefs().getInt("cabinetType", 0).coerceIn(0, 1))
            background = rounded(Color.rgb(12, 37, 63), 12, Color.rgb(39, 105, 159), 1)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            dropDownVerticalOffset = dp(4)
            dropDownWidth = ViewGroup.LayoutParams.MATCH_PARENT
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
        cabinetQuantityInput = field("تعداد کابینت مشابه", "Number of identical cabinets", "cabinetQuantity", "۱")
        pairedRow(cabinetCard, fieldCell("عرض کابینت (mm)", "Cabinet width (mm)", widthInput), fieldCell("ارتفاع کابینت (mm)", "Cabinet height (mm)", heightInput))
        pairedRow(cabinetCard, fieldCell("عمق کابینت (mm)", "Cabinet depth (mm)", depthInput), fieldCell("ضخامت ورق بدنه", "Carcass thickness", thicknessInput))
        pairedRow(cabinetCard, fieldCell("تعداد طبقه", "Shelves", shelfInput), fieldCell("تعداد در (۰ تا ۲)", "Doors (0–2)", doorInput))
        pairedRow(cabinetCard, fieldCell("ارتفاع پاخور", "Plinth height", plinthInput), fieldCell("ضخامت پشت‌بند", "Back panel", backThicknessInput))
        addTo(cabinetCard, label("تعداد کابینت با همین مشخصات", "Number of cabinets with these same dimensions", 12f))
        addTo(cabinetCard, cabinetQuantityInput, 8)
        addTo(cabinetCard, label("سفارش چند کابینتی", "Multi-cabinet order", 14f), 4)
        addTo(cabinetCard, TextView(this).apply {
            text = tr("ابعاد کابینت فعلی را وارد کنید و «افزودن به سفارش» را بزنید. سپس مشخصات کابینت بعدی را وارد کنید.", "Enter the current cabinet dimensions and tap Add to order. Then enter the next cabinet's dimensions.")
            textSize = 11f; setTextColor(muted); setPadding(0, 0, 0, dp(7))
        }, 4)
        orderListView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        addTo(cabinetCard, orderListView, 6)
        addTo(cabinetCard, button("＋ افزودن کابینت فعلی به سفارش", "+ Add current cabinet to order") {
            val cw = number(widthInput.text.toString())
            val ch = number(heightInput.text.toString())
            val cd = number(depthInput.text.toString())
            val ct = number(thicknessInput.text.toString())
            val cs = number(shelfInput.text.toString())?.toInt()
            val cdoors = number(doorInput.text.toString())?.toInt()
            val cp = number(plinthInput.text.toString())
            val cb = number(backThicknessInput.text.toString())
            val cq = number(cabinetQuantityInput.text.toString())?.toInt()
            if (cw == null || ch == null || cd == null || ct == null || cs == null || cdoors == null || cp == null || cb == null || cq == null || cw <= 0 || ch <= 0 || cd <= 0 || ct <= 0 || cs !in 0..30 || cdoors !in 0..2 || cp < 0 || cb < 0 || cq !in 1..100) {
                Toast.makeText(this, tr("ابتدا ابعاد و تعداد را درست وارد کنید.", "Enter valid dimensions and quantity first."), Toast.LENGTH_LONG).show()
            } else {
                val idx = orderCabinets.size + 1
                val spec = CabinetSpec(
                    id = "cabinet_$idx", name = if (english) "Cabinet $idx" else "کابینت ${faDigits(idx.toString())}",
                    cabinetType = if (cabinetSpinner.selectedItemPosition == 0) CabinetType.BASE else CabinetType.WALL,
                    widthMm = cw, heightMm = ch, depthMm = cd, boardThicknessMm = ct,
                    shelfCount = cs, doorCount = cdoors, quantity = cq, plinthMm = cp,
                    includeBack = includeBackCheck.isChecked, backThicknessMm = cb, stockType = "MDF", grainSensitive = true
                )
                orderCabinets += spec
                refreshOrderList()
                Toast.makeText(this, tr("${spec.name} به سفارش اضافه شد.", "${spec.name} added to order."), Toast.LENGTH_SHORT).show()
            }
        }, 8)
        addTo(cabinetCard, button("پاک کردن سفارش چند کابینتی", "Clear multi-cabinet order") {
            orderCabinets.clear(); refreshOrderList()
            Toast.makeText(this, tr("سفارش پاک شد؛ محاسبه دوباره از مشخصات فعلی انجام می‌شود.", "Order cleared; calculation will use current dimensions."), Toast.LENGTH_SHORT).show()
        }, 8)

        customPartsToggleButton = button("＋ قطعات سفارشی (۰)", "+ Custom parts (0)") {
            customPartsSection.visibility = if (customPartsSection.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            updateCustomPartsToggleLabel()
        }.apply {
            minHeight = dp(44)
            textSize = 13f
        }
        addTo(cabinetCard, customPartsToggleButton, 7)
        customPartsSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        addTo(cabinetCard, customPartsSection, 2)
        addTo(customPartsSection, TextView(this).apply {
            text = tr("قطعهٔ مستقل یا وابسته به ابعاد کابینت اضافه کنید.", "Add a standalone part or one linked to cabinet dimensions.")
            textSize = 11f; setTextColor(muted); setPadding(0, 0, 0, dp(5))
        }, 3)
        customPartNameInput = field("نام قطعه", "Part name", "customPartName", "لبه جلوی طبقه")
        customPartLengthInput = field("طول (mm)", "Length (mm)", "customPartLength", "۸۰۰")
        customPartWidthInput = field("عرض (mm)", "Width (mm)", "customPartWidth", "۵۰")
        customPartQtyInput = field("تعداد", "Quantity", "customPartQty", "۴")
        customPartThicknessInput = field("ضخامت ورق (mm)", "Board thickness (mm)", "customPartThickness", "۱۶")
        pairedRow(customPartsSection, fieldCell("نام قطعه", "Part name", customPartNameInput), fieldCell("تعداد", "Quantity", customPartQtyInput))
        pairedRow(customPartsSection, fieldCell("طول (mm)", "Length (mm)", customPartLengthInput), fieldCell("عرض (mm)", "Width (mm)", customPartWidthInput))
        addTo(customPartsSection, fieldCell("ضخامت ورق (mm)", "Board thickness (mm)", customPartThicknessInput), 6)
        customPartModeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@buildScreen, android.R.layout.simple_spinner_dropdown_item,
                if (english) listOf("Standalone part", "Linked to cabinet") else listOf("قطعه مستقل", "وابسته به کابینت"))
        }
        addTo(customPartsSection, label("نوع قطعه", "Part type", 12f), 2)
        addTo(customPartsSection, customPartModeSpinner, 5)
        customPartCabinetSpinner = Spinner(this)
        customPartDimensionSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@buildScreen, android.R.layout.simple_spinner_dropdown_item,
                if (english) listOf("Use cabinet width", "Use cabinet height", "Use cabinet depth") else listOf("طول برابر عرض کابینت", "طول برابر ارتفاع کابینت", "طول برابر عمق کابینت"))
        }
        addTo(customPartsSection, label("برای قطعه وابسته: کابینت و بُعد مرجع", "For linked part: cabinet and reference dimension", 12f), 2)
        addTo(customPartsSection, customPartCabinetSpinner, 3)
        addTo(customPartsSection, customPartDimensionSpinner, 6)
        customPartsListView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        addTo(customPartsSection, customPartsListView, 5)
        addTo(customPartsSection, button("＋ افزودن قطعه سفارشی", "+ Add custom part") {
            val name = customPartNameInput.text.toString().trim()
            val length = number(customPartLengthInput.text.toString())
            val width = number(customPartWidthInput.text.toString())
            val qty = number(customPartQtyInput.text.toString())?.toInt()
            val thickness = number(customPartThicknessInput.text.toString())
            val linked = customPartModeSpinner.selectedItemPosition == 1
            val cabinetIndex = customPartCabinetSpinner.selectedItemPosition
            if (name.isBlank() || length == null || width == null || qty == null || thickness == null || length <= 0 || width <= 0 || qty !in 1..1000 || thickness <= 0 || (linked && cabinetIndex !in orderCabinets.indices)) {
                Toast.makeText(this, tr("نام، ابعاد، ضخامت و تعداد را بررسی کنید. برای قطعه وابسته ابتدا کابینت را به سفارش اضافه کنید.", "Check name, dimensions, thickness and quantity. Add a cabinet before linking a part."), Toast.LENGTH_LONG).show()
            } else {
                val linkedCabinet = if (linked) orderCabinets[cabinetIndex] else null
                val dimension = if (linked) customPartDimensionSpinner.selectedItemPosition else -1
                customParts += CustomOrderPart(name, length, width, qty, thickness, linkedCabinet?.id, dimension)
                refreshCustomPartsList()
                Toast.makeText(this, tr("قطعه به سفارش اضافه شد.", "Custom part added to order."), Toast.LENGTH_SHORT).show()
            }
        }, 7)
        addTo(customPartsSection, button("پاک کردن قطعات سفارشی", "Clear custom parts") {
            customParts.clear(); refreshCustomPartsList()
            Toast.makeText(this, tr("قطعات سفارشی پاک شدند.", "Custom parts cleared."), Toast.LENGTH_SHORT).show()
        }, 8)

        val calculateButton = button("محاسبه و نمایش نقشه برش  →", "Calculate & view cutting plan  →") {
            saveSettings()
            calculate()
            showPage(1)
        }.apply {
            textSize = 16f
            minHeight = dp(58)
            background = AppTheme.primaryButton(this@buildScreen)
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
                addView(TextView(this@buildScreen).apply { text = tr(fa, en); textSize = 10f; setTextColor(AppTheme.TEXT_SECONDARY); gravity = Gravity.CENTER; setPadding(0, dp(3), 0, 0) })
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
        val sheetNavigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        previousSheetButton = button("‹ ورق قبلی", "‹ Previous sheet") {
            if (selectedSheetIndex > 0) {
                selectedSheetIndex--
                updateSheetNavigation()
                layoutView.invalidate()
            }
        }.apply { textSize = 12f }
        sheetCounterLabel = TextView(this).apply {
            text = tr("پس از محاسبه", "Calculate first")
            textSize = 13f
            setTextColor(ink)
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
        }
        nextSheetButton = button("ورق بعدی ›", "Next sheet ›") {
            if (selectedSheetIndex < latestSheetCount - 1) {
                selectedSheetIndex++
                updateSheetNavigation()
                layoutView.invalidate()
            }
        }.apply { textSize = 12f }
        sheetNavigation.addView(previousSheetButton, LinearLayout.LayoutParams(0, dp(42), 1f))
        sheetNavigation.addView(sheetCounterLabel, LinearLayout.LayoutParams(0, dp(42), 1f))
        sheetNavigation.addView(nextSheetButton, LinearLayout.LayoutParams(0, dp(42), 1f))
        layoutCard.addView(sheetNavigation, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(2) })
        layoutCard.addView(TextView(this).apply {
            text = tr("برای جابه‌جایی بین ورق‌ها، روی نقشه به چپ یا راست بکشید.", "Swipe left or right on the sheet to navigate.")
            textSize = 11f; setTextColor(muted); gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(7))
        }, LinearLayout.LayoutParams(-1, -2))
        layoutView = SheetLayoutView(
            context = this,
            placements = { latestPlacements },
            sheetWidth = { latestSheetW },
            sheetHeight = { latestSheetH },
            selectedSheet = { selectedSheetIndex },
            sheetCount = { latestSheetCount },
            onSelectedSheetChanged = { selectedSheetIndex = it; updateSheetNavigation() },
            partColor = { partColor(it) },
            translate = { fa, en -> tr(fa, en) },
            dp = { dp(it) }
        ).apply { background = rounded(Color.rgb(4, 19, 34), 14, AppTheme.CARD_BORDER, 1) }
        layoutCard.addView(layoutView, LinearLayout.LayoutParams(-1, dp(270)))
        updateSheetNavigation()
        val partsCard = addSection(resultsPage, "فهرست قطعات و جزئیات", "Parts list & details")
        partsList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.rgb(5, 22, 39), 13, Color.rgb(25, 79, 124), 1)
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        partsList.addView(TextView(this).apply {
            text = tr("پس از محاسبه، فهرست رنگی قطعات اینجا نمایش داده می‌شود.", "After calculation, the color-coded parts list appears here.")
            textSize = 12f
            setTextColor(ink)
            setPadding(dp(8), dp(10), dp(8), dp(10))
        })
        addTo(partsCard, partsList, 8)
        resultText = TextView(this).apply {
            text = tr("جزئیات محاسبه پس از انجام محاسبه نمایش داده می‌شود.", "Calculation details appear here after calculating.")
            textSize = 12f
            setTextColor(ink)
            setPadding(dp(12), dp(12), dp(12), dp(12))
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
            background = AppTheme.navBackground(this@buildScreen)
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
        refreshOrderList()
        refreshCustomPartsList()
        setContentView(root)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            root.setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(0, 0, 0, insets.systemWindowInsetBottom + dp(6))
                insets
            }
            root.requestApplyInsets()
        }
        showPage(0)
    }
