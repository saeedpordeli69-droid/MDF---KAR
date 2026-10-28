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

        // PRODUCT TYPE: first UI step toward supporting all MDF-KAR product families.
        // Kept separate from cabinetSpinner so existing cabinet calculations remain unchanged.
        val productTypeCard = addSection(
            homePage,
            "نوع محصول",
            "Product type",
            "نوع محصول موردنظر را انتخاب کنید",
            "Choose the product family you want to create"
        )
        val productTypeSpinner = Spinner(this).apply {
            val productTypes = if (english) arrayOf(
                "Cabinet", "Wardrobe", "Bookcase / Shelving", "Desk / Table",
                "Shop counter", "Reception counter", "TV wall / Decor",
                "Bathroom vanity", "Custom product"
            ) else arrayOf(
                "کابینت", "کمد", "کتابخانه و قفسه", "میز",
                "پیشخوان فروشگاهی", "پیشخوان اداری", "دیوار تلویزیون و دکور",
                "کابینت روشویی", "محصول سفارشی"
            )
            adapter = object : ArrayAdapter<String>(this@buildScreen, android.R.layout.simple_spinner_item, productTypes) {
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
        }
        addTo(productTypeCard, productTypeSpinner, 2)
        val productTypeHint = TextView(this).apply {
            text = tr(
                "برای کابینت از فرم ابعاد کابینت استفاده کنید. سایر محصولات فعلاً با ثبت قطعات و نقشهٔ برش محاسبه می‌شوند.",
                "Cabinets use the cabinet form. Other product families currently use entered parts and the cutting-plan workflow."
            )
            textSize = 11f
            setTextColor(AppTheme.TEXT_SECONDARY)
            setPadding(0, dp(4), 0, 0)
        }
        addTo(productTypeCard, productTypeHint, 1)

        val cabinetCard = addSection(homePage, "مشخصات کابینت", "Cabinet specifications", "نوع کابینت و ابعاد بدنه", "Cabinet type and carcass dimensions")
        val cabinetCardTitle = cabinetCard.getChildAt(0) as TextView
        val cabinetCardSubtitle = cabinetCard.getChildAt(1) as TextView
        cabinetSpinner = Spinner(this).apply {
            val cabinetTypes = if (english) arrayOf("Independent cutting", "Base cabinet", "Wall cabinet") else arrayOf("برش مستقل", "کابینت زمینی", "کابینت دیواری")
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
            setSelection(prefs().getInt("cabinetType", 0).coerceIn(0, 2))
            background = rounded(Color.rgb(12, 37, 63), 12, Color.rgb(39, 105, 159), 1)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            dropDownVerticalOffset = dp(4)
            dropDownWidth = ViewGroup.LayoutParams.MATCH_PARENT
        }
        addTo(cabinetCard, cabinetSpinner, 8)
        fun applyCuttingMode(position: Int) {
            val independent = position == 0
            customPartsSection.visibility = if (independent) View.VISIBLE else View.GONE
            customPartsToggleButton.visibility = if (independent) View.GONE else View.VISIBLE
            customPartModeSpinner.setSelection(0)
            // Child indexes 4..8 are the linked/standalone mode controls. Keep the
            // thickness field, registered-parts list, Add button and Clear button visible.
            for (index in 4..8) {
                if (index < customPartsSection.childCount) {
                    customPartsSection.getChildAt(index).visibility = if (independent) View.GONE else View.VISIBLE
                }
            }
            if (independent) refreshIndependentPartsList() else refreshCustomPartsList()
            updateCustomPartsToggleLabel()
        }

        cabinetSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                // The spinner can fire before its form is built; apply again after setup below.
                runCatching { applyCuttingMode(position) }
            }
        }
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
                    cabinetType = if (cabinetSpinner.selectedItemPosition == 2) CabinetType.WALL else CabinetType.BASE,
                    widthMm = cw, heightMm = ch, depthMm = cd, boardThicknessMm = ct,
                    shelfCount = cs, doorCount = cdoors, quantity = cq, plinthMm = cp,
                    includeBack = includeBackCheck.isChecked, backThicknessMm = cb, stockType = "MDF", grainSensitive = true
                )
                orderCabinets += spec
                refreshOrderList()
                Toast.makeText(this, tr("${spec.name} به سفارش اضافه شد.", "${spec.name} added to order."), Toast.LENGTH_SHORT).show()
            }
        }, 8)
        addTo(cabinetCard, button("پاک کردن کل سفارش", "Clear entire order") {
            clearRegisteredOrder()
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
        addTo(customPartsSection, button("＋ افزودن قطعه", "+ Add part") {
            val name = customPartNameInput.text.toString().trim()
            val length = number(customPartLengthInput.text.toString())
            val width = number(customPartWidthInput.text.toString())
            val qty = number(customPartQtyInput.text.toString())?.toInt()
            val thickness = number(customPartThicknessInput.text.toString())
            val independent = cabinetSpinner.selectedItemPosition == 0
            val linked = !independent && customPartModeSpinner.selectedItemPosition == 1
            val cabinetIndex = customPartCabinetSpinner.selectedItemPosition
            if (name.isBlank() || length == null || width == null || qty == null || thickness == null || length <= 0 || width <= 0 || qty !in 1..1000 || thickness <= 0 || (linked && cabinetIndex !in orderCabinets.indices)) {
                Toast.makeText(this, tr("نام، طول، عرض، ضخامت و تعداد معتبر وارد کنید. برای قطعه وابسته ابتدا کابینت را به سفارش اضافه کنید.", "Enter a name and valid length, width, thickness and quantity. Add a cabinet before linking a part."), Toast.LENGTH_LONG).show()
            } else if (independent) {
                independentParts += CustomOrderPart(name, length, width, qty, thickness, null, -1)
                refreshIndependentPartsList()
                Toast.makeText(this, tr("قطعه مستقل ثبت شد.", "Independent part registered."), Toast.LENGTH_SHORT).show()
            } else {
                val linkedCabinet = if (linked) orderCabinets[cabinetIndex] else null
                val dimension = if (linked) customPartDimensionSpinner.selectedItemPosition else -1
                customParts += CustomOrderPart(name, length, width, qty, thickness, linkedCabinet?.id, dimension)
                refreshCustomPartsList()
                Toast.makeText(this, tr("قطعه به سفارش اضافه شد.", "Custom part added to order."), Toast.LENGTH_SHORT).show()
            }
        }, 7)
        addTo(customPartsSection, button("پاک کردن قطعات", "Clear parts") {
            if (cabinetSpinner.selectedItemPosition == 0) {
                independentParts.clear(); refreshIndependentPartsList()
            } else {
                customParts.clear(); refreshCustomPartsList()
            }
            Toast.makeText(this, tr("فهرست قطعات پاک شد.", "Parts list cleared."), Toast.LENGTH_SHORT).show()
        }, 8)

        // Apply the selected mode after all controls have been created. Previously the first
        // spinner callback ran too early and the independent-mode code hid the Add/list views.
        applyCuttingMode(cabinetSpinner.selectedItemPosition)

        // Non-cabinet product form: dimensions of the whole item generate a cutting list automatically.
        val productDetailsCard = addSection(
            homePage, "ابعاد کلی محصول", "Overall product dimensions",
            "ابعاد کلی را وارد کنید؛ قطعات و نقشه برش به‌صورت خودکار ساخته می‌شوند.",
            "Enter overall dimensions; components and the cutting plan are generated automatically."
        ).apply { visibility = View.GONE }
        productWidthInput = field("عرض کلی (mm)", "Overall width (mm)", "productWidth", "۱۲۰۰")
        productHeightInput = field("ارتفاع کلی (mm)", "Overall height (mm)", "productHeight", "۱۸۰۰")
        productDepthInput = field("عمق کلی (mm)", "Overall depth (mm)", "productDepth", "۵۰۰")
        productThicknessInput = field("ضخامت ورق (mm)", "Board thickness (mm)", "productThickness", "۱۶")
        productShelfInput = field("تعداد طبقه", "Number of shelves", "productShelves", "۳")
        productDoorInput = field("تعداد در", "Number of doors", "productDoors", "۲")
        productQuantityInput = field("تعداد محصول مشابه", "Number of identical products", "productQuantity", "۱")
        pairedRow(productDetailsCard, fieldCell("عرض کلی (mm)", "Overall width (mm)", productWidthInput), fieldCell("ارتفاع کلی (mm)", "Overall height (mm)", productHeightInput))
        pairedRow(productDetailsCard, fieldCell("عمق کلی (mm)", "Overall depth (mm)", productDepthInput), fieldCell("ضخامت ورق (mm)", "Board thickness (mm)", productThicknessInput))
        val productShelvesField = fieldCell("تعداد طبقه", "Number of shelves", productShelfInput)
        addTo(productDetailsCard, productShelvesField, 5)
        val productDoorsField = fieldCell("تعداد در", "Number of doors", productDoorInput)
        addTo(productDetailsCard, productDoorsField, 5)
        addTo(productDetailsCard, label("تعداد محصول با همین ابعاد", "Quantity with these dimensions", 12f), 2)
        addTo(productDetailsCard, productQuantityInput, 5)
        productIncludeBackCheck = CheckBox(this).apply {
            text = tr("ساخت قطعهٔ پشت‌بند", "Include a back panel")
            isChecked = true
            setTextColor(AppTheme.TEXT)
            buttonTintList = android.content.res.ColorStateList.valueOf(AppTheme.ACCENT)
        }
        addTo(productDetailsCard, productIncludeBackCheck, 3)
        val productRecipeHint = TextView(this).apply {
            textSize = 12f
            setTextColor(AppTheme.TEXT_SECONDARY)
            setPadding(dp(3), dp(4), dp(3), dp(3))
        }
        addTo(productDetailsCard, productRecipeHint, 2)

        // Modular structure controls: each module changes both the live preview and generated cutting list.
        val structureOptionsCard = addSection(
            productDetailsCard, "چیدمان اجزای محصول", "Product component layout",
            "برای پیشخوان و کتابخانه، اجزای واقعی را انتخاب کنید.",
            "Choose actual modules for counters and bookcases."
        )
        fun styledSpinner(faItems: Array<String>, enItems: Array<String>): Spinner = Spinner(this).apply {
            adapter = object : ArrayAdapter<String>(this@buildScreen, android.R.layout.simple_spinner_item, if (english) enItems else faItems) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                    (super.getView(position, convertView, parent) as TextView).apply {
                        setTextColor(AppTheme.TEXT); textSize = 14f; setPadding(dp(10), dp(7), dp(10), dp(7))
                    }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                    (super.getDropDownView(position, convertView, parent) as TextView).apply {
                        setTextColor(AppTheme.TEXT); textSize = 14f; setBackgroundColor(Color.rgb(7, 24, 43)); setPadding(dp(12), dp(10), dp(12), dp(10))
                    }
            }.also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            background = rounded(Color.rgb(12, 37, 63), 12, Color.rgb(39, 105, 159), 1)
        }
        val counterOptions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        addTo(structureOptionsCard, counterOptions, 4)
        addTo(counterOptions, label("بخش سمت چپ پیشخوان", "Counter left module", 12f), 2)
        productLeftModuleSpinner = styledSpinner(
            arrayOf("کشوها", "کمد درب‌دار", "قفسه باز", "فضای خالی"),
            arrayOf("Drawers", "Cabinet", "Open shelves", "Open space")
        )
        addTo(counterOptions, productLeftModuleSpinner, 5)
        addTo(counterOptions, label("بخش سمت راست پیشخوان", "Counter right module", 12f), 2)
        productRightModuleSpinner = styledSpinner(
            arrayOf("کشوها", "کمد درب‌دار", "قفسه باز", "فضای خالی"),
            arrayOf("Drawers", "Cabinet", "Open shelves", "Open space")
        )
        productRightModuleSpinner.setSelection(1)
        addTo(counterOptions, productRightModuleSpinner, 5)
        addTo(counterOptions, label("تعداد کشو در هر بخش کشودار", "Drawers per drawer module", 12f), 2)
        productDrawerCountInput = field("تعداد کشو", "Drawer count", "productDrawerCount", "۲")
        addTo(counterOptions, productDrawerCountInput, 4)

        val bookcaseOptions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        addTo(structureOptionsCard, bookcaseOptions, 4)
        addTo(bookcaseOptions, label("ساختار قسمت پایین کتابخانه", "Bookcase lower section", 12f), 2)
        bookcaseBaseTypeSpinner = styledSpinner(
            arrayOf("کمد درب‌دار", "کشو", "قفسه باز"),
            arrayOf("Closed cabinet", "Drawers", "Open shelves")
        )
        addTo(bookcaseOptions, bookcaseBaseTypeSpinner, 5)
        addTo(bookcaseOptions, label("ارتفاع قسمت پایین (mm)", "Lower section height (mm)", 12f), 2)
        bookcaseBaseHeightInput = field("ارتفاع قسمت پایین", "Lower section height", "bookcaseBaseHeight", "۶۰۰")
        addTo(bookcaseOptions, bookcaseBaseHeightInput, 4)

        productPreviewView = ProductPreviewView(this).apply {
            contentDescription = tr("پیش‌نمایش شماتیک و زنده محصول", "Live schematic product preview")
            minimumHeight = dp(220)
        }
        addTo(productDetailsCard, TextView(this).apply {
            text = tr("پیش‌نمایش طراحی", "Design preview")
            textSize = 15f; setTextColor(AppTheme.ACCENT); setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(2), dp(6), dp(2), dp(4))
        }, 3)
        productDetailsCard.addView(productPreviewView, LinearLayout.LayoutParams(-1, dp(245)).apply { bottomMargin = dp(10) })
        productCutListPreview = TextView(this).apply {
            text = tr("فهرست قطعات پیشنهادی پس از واردکردن ابعاد نمایش داده می‌شود.", "The suggested cut list appears after valid dimensions are entered.")
            textSize = 11f
            setTextColor(AppTheme.TEXT_SECONDARY)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(Color.rgb(7, 27, 49), 10, Color.rgb(35, 100, 150), 1)
        }
        addTo(productDetailsCard, label("فهرست زنده قطعات (میلی‌متر)", "Live cut list (mm)", 13f), 3)
        productDetailsCard.addView(productCutListPreview, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        val productOrderCard = addSection(
            productDetailsCard,
            "فهرست مشترک سفارش",
            "Shared order list",
            "محصول را ثبت کنید؛ سپس می‌توانید محصول بعدی یا کابینت دیگری را هم به همین سفارش اضافه کنید.",
            "Register this product, then add another product or cabinet to the same order."
        )
        productOrderListView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        addTo(productOrderCard, productOrderListView, 5)
        addTo(productOrderCard, button("＋ افزودن این محصول به سفارش", "+ Add this product to order") {
            addCurrentProductToOrder()
        }, 6)
        addTo(productOrderCard, button("پاک کردن کل سفارش", "Clear entire order") {
            clearRegisteredOrder()
        }, 3)
        refreshProductOrderList()

        fun updateProductPreview() {
            val w = number(productWidthInput.text.toString()) ?: 1200.0
            val h = number(productHeightInput.text.toString()) ?: 900.0
            val d = number(productDepthInput.text.toString()) ?: 500.0
            val t = number(productThicknessInput.text.toString()) ?: 16.0
            val shelfCount = number(productShelfInput.text.toString())?.toInt() ?: 3
            val doorCount = number(productDoorInput.text.toString())?.toInt() ?: 2
            val drawers = (number(productDrawerCountInput.text.toString())?.toInt() ?: 2).coerceIn(1, 8)
            val baseH = number(bookcaseBaseHeightInput.text.toString()) ?: 600.0
            productPreviewView.configure(
                selectedProductTypeIndex, w, h, d, shelfCount, doorCount,
                productLeftModuleSpinner.selectedItemPosition, productRightModuleSpinner.selectedItemPosition,
                drawers, bookcaseBaseTypeSpinner.selectedItemPosition, baseH, english
            )
            if (selectedProductIsCabinet) {
                productCutListPreview.text = tr("برای نمایش فهرست قطعات، ابتدا یک محصول غیرکابینتی انتخاب کنید.", "Select a non-cabinet product to preview its cut list.")
            } else {
                val generated = generateAutomaticProductParts(showErrors = false)
                productCutListPreview.text = if (generated == null) {
                    tr("ابعاد یا تنظیمات ماژول هنوز معتبر نیست.", "Dimensions or module settings are not valid yet.")
                } else {
                    val lines = generated.take(16).map { part ->
                        "• ${part.name}: ${if (english) fmt(part.lengthMm) else faDigits(fmt(part.lengthMm))} × ${if (english) fmt(part.widthMm) else faDigits(fmt(part.widthMm))} mm  |  ${tr("تعداد", "qty")} ${if (english) part.quantity.toString() else faDigits(part.quantity.toString())}  |  ${if (english) fmt(part.thicknessMm) else faDigits(fmt(part.thicknessMm))} mm"
                    }
                    val remaining = generated.size - 16
                    val more = if (remaining > 0) "\n… ${if (english) remaining.toString() else faDigits(remaining.toString())} ${tr("نوع قطعه دیگر", "more part types")}" else ""
                    lines.joinToString("\n") + more
                }
            }
        }
        val previewWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateProductPreview() }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        }
        listOf(productWidthInput, productHeightInput, productDepthInput, productThicknessInput, productShelfInput, productDoorInput, productQuantityInput, productDrawerCountInput, bookcaseBaseHeightInput).forEach { it.addTextChangedListener(previewWatcher) }
        productIncludeBackCheck.setOnCheckedChangeListener { _, _ -> updateProductPreview() }
        listOf(productLeftModuleSpinner, productRightModuleSpinner, bookcaseBaseTypeSpinner).forEach { spinner ->
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { updateProductPreview() }
            }
        }

        var lastProductDefaults = listOf(1200.0, 1800.0, 500.0)
        fun applyProductSelection(position: Int) {
            selectedProductTypeIndex = position
            selectedProductIsCabinet = position == 0
            if (position == 0) {
                // Keep the established cabinet form unchanged.
                cabinetCard.visibility = View.VISIBLE
                productDetailsCard.visibility = View.GONE
                if (cabinetSpinner.selectedItemPosition == 0) cabinetSpinner.setSelection(1)
                runCatching { applyCuttingMode(cabinetSpinner.selectedItemPosition) }
                cabinetCardTitle.text = "◆  ${tr("مشخصات کابینت", "Cabinet specifications")}"
                cabinetCardSubtitle.text = tr("نوع کابینت و ابعاد بدنه", "Cabinet type and carcass dimensions")
                productTypeHint.text = tr(
                    "کابینت: ابعاد بدنه را وارد کنید و کابینت را به سفارش اضافه کنید.",
                    "Cabinet: enter carcass dimensions and add the cabinet to the order."
                )
            } else {
                // Apply sensible dimensions for the newly selected family only when the user has
                // not changed the previous family's defaults; preserve manually entered dimensions.
                val defaults = when (position) {
                    1 -> listOf(1200.0, 2200.0, 600.0) // wardrobe
                    2 -> listOf(1000.0, 1800.0, 300.0) // bookcase
                    3 -> listOf(1200.0, 750.0, 600.0) // desk/table
                    4 -> listOf(1500.0, 1000.0, 600.0) // shop counter
                    5 -> listOf(1800.0, 1100.0, 700.0) // reception counter
                    6 -> listOf(1800.0, 2000.0, 300.0) // TV wall
                    7 -> listOf(900.0, 800.0, 500.0) // vanity
                    else -> listOf(1200.0, 900.0, 500.0)
                }
                val dimensionInputs = listOf(productWidthInput, productHeightInput, productDepthInput)
                dimensionInputs.forEachIndexed { index, input ->
                    if (number(input.text.toString()) == lastProductDefaults[index]) {
                        val value = defaults[index].toInt().toString()
                        input.setText(if (english) value else faDigits(value))
                    }
                }
                lastProductDefaults = defaults
                productShelvesField.visibility = if (position in listOf(1, 2, 4, 5, 6, 7, 8)) View.VISIBLE else View.GONE
                productDoorsField.visibility = if (position in listOf(1, 7, 8)) View.VISIBLE else View.GONE
                productIncludeBackCheck.visibility = if (position in listOf(1, 2, 4, 5, 7, 8)) View.VISIBLE else View.GONE
                // Non-cabinet products have their own overall-dimensions form and auto-generated parts.
                cabinetCard.visibility = View.GONE
                productDetailsCard.visibility = View.VISIBLE
                structureOptionsCard.visibility = if (position in listOf(2, 4, 5)) View.VISIBLE else View.GONE
                counterOptions.visibility = if (position in listOf(4, 5)) View.VISIBLE else View.GONE
                bookcaseOptions.visibility = if (position == 2) View.VISIBLE else View.GONE
                cabinetSpinner.setSelection(0)
                runCatching { applyCuttingMode(0) }
                val selectedName = productTypeSpinner.selectedItem?.toString().orEmpty()
                val productNamesFa = arrayOf("کابینت", "کمد", "کتابخانه و قفسه", "میز", "پیشخوان فروشگاهی", "پیشخوان اداری", "دیوار تلویزیون و دکور", "کابینت روشویی", "محصول سفارشی")
                val productNamesEn = arrayOf("Cabinet", "Wardrobe", "Bookcase / Shelving", "Desk / Table", "Shop counter", "Reception counter", "TV wall / Decor", "Bathroom vanity", "Custom product")
                val productName = if (english) productNamesEn[position.coerceIn(0, productNamesEn.lastIndex)] else productNamesFa[position.coerceIn(0, productNamesFa.lastIndex)]
                val recipeFa = when (position) {
                    1 -> "قطعات کمد: دو بغل، بالا و پایین، طبقه‌ها، درها و در صورت انتخاب پشت‌بند."
                    2 -> "قطعات کتابخانه: بغل‌ها، بالا و پایین، طبقه‌ها، تقسیم‌کننده و پشت‌بند."
                    3 -> "قطعات میز: صفحه، چهار پایه و تیرک‌های زیر صفحه."
                    4 -> "قطعات پیشخوان فروشگاهی: نمای جلو، دو بغل، صفحه روی پیشخوان و طبقه‌ها."
                    5 -> "قطعات کانتر اداری: نمای جلو، دو بغل، صفحه کار و طبقه‌های داخلی."
                    6 -> "قطعات دکور: پنل اصلی، نوارهای عمودی و طبقه‌های دکور."
                    7 -> "قطعات روشویی: بغل‌ها، بالا و پایین، طبقه‌ها، درها، صفحه و پشت‌بند."
                    else -> "قطعات محصول سفارشی بر اساس الگوی بدنه، طبقه‌ها و درها ساخته می‌شوند."
                }
                val recipeEn = when (position) {
                    1 -> "Wardrobe: sides, top/bottom, shelves, doors and optional back panel."
                    2 -> "Bookcase: sides, top/bottom, shelves, divider and back panel."
                    3 -> "Desk/table: top, four leg panels and support aprons."
                    4 -> "Shop counter: front, two sides, countertop and shelves."
                    5 -> "Reception counter: front, two sides, worktop and inner shelves."
                    6 -> "TV wall/decor: main panel, vertical strips and display shelves."
                    7 -> "Vanity: sides, top/bottom, shelves, doors, top and optional back panel."
                    else -> "Custom product: carcass panels, shelves, doors and optional back panel."
                }
                cabinetCardTitle.text = "◆  $productName"
                cabinetCardSubtitle.text = tr("فرم تخصصی و تولید خودکار قطعات", "Product-specific form and automatic part generation")
                productRecipeHint.text = tr(recipeFa, recipeEn)
                productTypeHint.text = tr(
                    "ابعاد کلی $productName را وارد کنید. برنامه قطعات پیشنهادی را می‌سازد و در صفحه بعد نقشه برش را نمایش می‌دهد.",
                    "Enter the overall dimensions for $productName. The app generates suggested components and shows the cutting layout on the next page."
                )
                updateProductPreview()
            }
        }
        productTypeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyProductSelection(position)
            }
        }
        // Ensure the initial Cabinet selection matches the visible form instead of leaving
        // the independent-cutting form selected under a Cabinet heading.
        applyProductSelection(productTypeSpinner.selectedItemPosition)
        updateProductPreview()

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
        val pricingCard = addSection(resultsPage, "برآورد قیمت سفارش", "Order price estimate", "قیمت‌ها را به تومان وارد کنید؛ هزینه ورق بر اساس تعداد ورق کامل محاسبه می‌شود.", "Enter prices in toman; board cost is based on complete sheet count.")
        fun priceInput(hintFa: String, hintEn: String): EditText = EditText(this).apply {
            this.hint = tr(hintFa, hintEn)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            textSize = 14f
            setTextColor(ink)
            setHintTextColor(muted)
            background = rounded(Color.rgb(10, 31, 53), 12, Color.rgb(43, 102, 153), 1)
            setPadding(dp(12), dp(9), dp(12), dp(9))
        }
        boardPriceInput = priceInput("قیمت هر ورق کامل MDF (تومان)", "Price per full MDF sheet (toman)")
        edgePriceInput = priceInput("قیمت هر متر نوار لبه (تومان)", "Price per metre of edge banding (toman)")
        hardwareCostInput = priceInput("هزینه کل یراق‌آلات (تومان)", "Total hardware cost (toman)")
        laborCostInput = priceInput("دستمزد ساخت و نصب (تومان)", "Fabrication and installation labor (toman)")
        transportCostInput = priceInput("هزینه حمل‌ونقل (تومان)", "Transport cost (toman)")
        otherCostInput = priceInput("هزینه‌های متفرقه (تومان)", "Other costs (toman)")
        profitPercentInput = priceInput("درصد سود (%)", "Profit percentage (%)")
        listOf(
            boardPriceInput to ("قیمت هر ورق کامل MDF (تومان)" to "Price per full MDF sheet (toman)"),
            edgePriceInput to ("قیمت هر متر نوار لبه (تومان)" to "Price per metre of edge banding (toman)"),
            hardwareCostInput to ("هزینه کل یراق‌آلات (تومان)" to "Total hardware cost (toman)"),
            laborCostInput to ("دستمزد ساخت و نصب (تومان)" to "Fabrication and installation labor (toman)"),
            transportCostInput to ("هزینه حمل‌ونقل (تومان)" to "Transport cost (toman)"),
            otherCostInput to ("هزینه‌های متفرقه (تومان)" to "Other costs (toman)"),
            profitPercentInput to ("درصد سود (%)" to "Profit percentage (%)")
        ).forEach { (field, labels) ->
            addTo(pricingCard, label(labels.first, labels.second, 12f), 3)
            addTo(pricingCard, field, 7)
        }
        priceEstimateText = TextView(this).apply {
            text = tr("پس از انجام محاسبه، تعداد ورق و مقدار نوار لبه در برآورد قیمت استفاده می‌شود.", "Calculate the order first to use sheet count and edge banding length in the estimate.")
            textSize = 13f
            setTextColor(ink)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(Color.rgb(5, 22, 39), 13, Color.rgb(25, 79, 124), 1)
        }
        addTo(pricingCard, priceEstimateText, 8)
        addTo(pricingCard, button("محاسبهٔ دوبارهٔ قیمت", "Recalculate price") { updatePriceEstimate() }, 2)
        val pricePrefs = prefs()
        listOf(
            boardPriceInput to "priceBoardSheet",
            edgePriceInput to "priceEdgeMeter",
            hardwareCostInput to "priceHardware",
            laborCostInput to "priceLabor",
            transportCostInput to "priceTransport",
            otherCostInput to "priceOther",
            profitPercentInput to "priceProfit"
        ).forEach { (field, key) ->
            field.setText(pricePrefs.getString(key, "0") ?: "0")
            field.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) { updatePriceEstimate(); saveSettings() }
            }
        }
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
