package ir.mdfkar.app

/** Product-family cut-list generation, kept separate from the activity UI code. */
/** Build a product-specific cut list from the same dimensions used by the live preview. */
internal fun MainActivity.generateAutomaticProductParts(showErrors: Boolean = true): List<CustomOrderPart>? {
        val w = number(productWidthInput.text.toString())
        val h = number(productHeightInput.text.toString())
        val d = number(productDepthInput.text.toString())
        val t = number(productThicknessInput.text.toString())
        val shelves = number(productShelfInput.text.toString())?.toInt()
        val doors = number(productDoorInput.text.toString())?.toInt()
        val quantity = number(productQuantityInput.text.toString())?.toInt()
        val drawerCount = if (isProductDrawerCountInputInitialized()) number(productDrawerCountInput.text.toString())?.toInt() ?: 2 else 2
        val leftModule = if (isProductLeftModuleSpinnerInitialized()) productLeftModuleSpinner.selectedItemPosition else 0
        val rightModule = if (isProductRightModuleSpinnerInitialized()) productRightModuleSpinner.selectedItemPosition else 1
        val bookcaseBase = if (isBookcaseBaseTypeSpinnerInitialized()) bookcaseBaseTypeSpinner.selectedItemPosition else 0
        val bookcaseBaseHeight = if (isBookcaseBaseHeightInputInitialized()) number(bookcaseBaseHeightInput.text.toString()) ?: (h ?: 900.0) * 0.33 else (h ?: 900.0) * 0.33
        val invalid = w == null || h == null || d == null || t == null || shelves == null || doors == null || quantity == null ||
            w <= 0 || h <= 0 || d <= 0 || t <= 0 || w <= 3 * t || h <= 2 * t || d <= t ||
            shelves !in 0..30 || doors !in 0..12 || quantity !in 1..100 || drawerCount !in 1..8 ||
            (selectedProductTypeIndex == 2 && (bookcaseBaseHeight <= 2 * t || bookcaseBaseHeight >= h))
        if (invalid) {
            if (showErrors) resultText.text = tr(
                "ابعاد کلی، ضخامت و تعداد محصول معتبر نیست. عرض و ارتفاع باید فضای کافی برای ضخامت ورق داشته باشند؛ طبقه ۰ تا ۳۰، در ۰ تا ۱۲ و تعداد محصول ۱ تا ۱۰۰ باشد.",
                "Invalid overall dimensions, board thickness or quantity. Allow enough room for board thickness; shelves 0–30, doors 0–12 and product quantity 1–100."
            )
            return null
        }

        // Values are now non-null and positive after the validation above.
        val width = w!!; val height = h!!; val depth = d!!; val thickness = t!!
        val shelfCount = shelves!!; val doorCount = doors!!; val productQty = quantity!!
        val parts = mutableListOf<CustomOrderPart>()
        var generationError = false
        fun addPart(fa: String, en: String, length: Double, partWidth: Double, count: Int, boardThickness: Double = thickness) {
            if (count > 0 && length > 0.0 && partWidth > 0.0 && boardThickness > 0.0) {
                parts += CustomOrderPart(tr(fa, en), length, partWidth, count * productQty, boardThickness)
            }
        }
        fun carcassSides(sideNameFa: String = "پنل کناری", sideNameEn: String = "Side panel") = addPart(sideNameFa, sideNameEn, height, depth, 2)
        fun carcassTopBottom(topFa: String = "پنل بالا و پایین", topEn: String = "Top/bottom panel") = addPart(topFa, topEn, width - 2 * thickness, depth, 2)
        fun addShelves(count: Int = shelfCount, fa: String = "طبقه", en: String = "Shelf", shelfWidth: Double = width - 2 * thickness) =
            addPart(fa, en, shelfWidth, depth, count)
        fun backPanel(backWidth: Double = width - 2 * thickness, backHeight: Double = height - 2 * thickness, count: Int = 1) {
            // A typical thin back panel is 3 mm stock, not the carcass board thickness.
            if (productIncludeBackCheck.isChecked) addPart("پشت‌بند ۳ میلی‌متری", "3 mm back panel", backHeight, backWidth, count, 3.0)
        }
        fun doorsForOpening(openingWidth: Double, openingHeight: Double, count: Int, fa: String = "درب", en: String = "Door") {
            if (count <= 0) return
            val gap = 2.0
            val doorWidth = (openingWidth - gap * (count + 1)) / count
            val doorHeight = openingHeight - 2 * gap
            if (doorWidth > 0 && doorHeight > 0) addPart(fa, en, doorHeight, doorWidth, count) else {
                generationError = true
                if (showErrors) resultText.text = tr("عرض یا ارتفاع برای تعداد انتخاب‌شدهٔ درها کافی نیست.", "The opening is too small for the selected number of doors.")
            }
        }
        fun drawerModule(clearWidth: Double, clearHeight: Double, drawerTotal: Int, prefixFa: String, prefixEn: String) {
            if (drawerTotal <= 0 || clearWidth <= 2 * thickness || clearHeight <= 0) return
            val gap = 2.0
            val frontWidth = clearWidth - 2 * gap
            val frontHeight = (clearHeight - (drawerTotal + 1) * gap) / drawerTotal
            // Allow 13 mm clearance per side for common drawer runners and 25 mm less depth for the box.
            val boxWidth = clearWidth - 2 * thickness - 26.0
            val boxDepth = depth - 50.0
            val boxHeight = frontHeight - 25.0
            if (frontWidth <= 0 || frontHeight < 45.0 || boxWidth <= 0 || boxDepth <= 0 || boxHeight <= 0) {
                generationError = true
                if (showErrors) resultText.text = tr(
                    "فضای انتخاب‌شده برای کشو با این تعداد یا ابعاد کافی نیست. عرض، ارتفاع، عمق یا تعداد کشوها را تغییر دهید.",
                    "The selected module is too small for this drawer count. Adjust width, height, depth or drawer count."
                )
                return
            }
            addPart("$prefixFa؛ نمای کشو", "$prefixEn drawer front", frontHeight, frontWidth, drawerTotal)
            addPart("$prefixFa؛ بغل کشو", "$prefixEn drawer side", boxDepth, boxHeight, drawerTotal * 2)
            addPart("$prefixFa؛ جلو و پشت جعبه کشو", "$prefixEn drawer front/back box", boxWidth, boxHeight, drawerTotal * 2)
            addPart("$prefixFa؛ کف کشو", "$prefixEn drawer bottom", boxDepth, boxWidth, drawerTotal, 3.0)
        }

        when (selectedProductTypeIndex) {
            1 -> { // Wardrobe: full-height sides, in-between top/bottom, internal shelves and overlay doors.
                carcassSides("بغل کمد", "Wardrobe side")
                carcassTopBottom("بالا و پایین کمد", "Wardrobe top/bottom")
                addShelves()
                doorsForOpening(width, height, doorCount)
                backPanel()
            }
            2 -> { // Bookcase: lower module plus a separate upper shelving zone.
                carcassSides("بغل کتابخانه", "Bookcase side")
                carcassTopBottom("بالا و کف کتابخانه", "Bookcase top/bottom")
                val baseHeight = bookcaseBaseHeight
                val upperHeight = height - baseHeight - thickness
                if (upperHeight <= thickness) {
                    if (showErrors) resultText.text = tr("ارتفاع بخش بالایی کتابخانه برای طبقه و تقسیم‌کننده کافی نیست.", "The upper bookcase section is too short for shelves and divider.")
                    return null
                }
                if (shelfCount > 0) addShelves(shelfCount, "طبقه بخش بالایی", "Upper bookcase shelf")
                addPart("طبقه روی ماژول پایین", "Lower module top shelf", width - 2 * thickness, depth, 1)
                addPart("تقسیم‌کننده عمودی بخش بالا", "Upper vertical divider", upperHeight, depth, 1)
                when (bookcaseBase) {
                    0 -> doorsForOpening(width - 2 * thickness, baseHeight - 2 * thickness, 2, "درب کمد پایین", "Lower cabinet door")
                    1 -> drawerModule(width - 2 * thickness, baseHeight - 2 * thickness, drawerCount, "کشوی پایین", "Lower drawer")
                    else -> addShelves(1, "طبقه باز بخش پایین", "Lower open shelf", width - 2 * thickness)
                }
                backPanel()
            }
            3 -> { // Desk/table. Height includes the top, so legs end below the top panel.
                addPart("صفحه روی میز", "Tabletop", width, depth, 1)
                addPart("پنل پایه میز", "Table leg panel", height - thickness, 90.0, 4)
                addPart("تیرک طولی زیر میز", "Long apron", width - 2 * thickness, 100.0, 2)
                addPart("تیرک عرضی زیر میز", "Short apron", depth - 2 * thickness, 100.0, 2)
            }
            4, 5 -> { // Counter: two clear modules separated by the center panel.
                val moduleWidth = (width - 3 * thickness) / 2.0
                if (moduleWidth <= 2 * thickness) {
                    if (showErrors) resultText.text = tr("عرض پیشخوان برای دو ماژول و ضخامت ورق کافی نیست.", "Counter width is too small for two modules and the selected board thickness.")
                    return null
                }
                addPart("بغل پیشخوان", "Counter side panel", height, depth, 2)
                addPart("تقسیم‌کننده مرکزی پیشخوان", "Counter center divider", height - 2 * thickness, depth, 1)
                addPart("کف ماژول", "Module bottom", moduleWidth, depth, 2)
                addPart(if (selectedProductTypeIndex == 4) "صفحه روی پیشخوان" else "صفحه کار کانتر", if (selectedProductTypeIndex == 4) "Shop counter top" else "Reception worktop", width, depth, 1)
                fun addModule(module: Int, sideFa: String, sideEn: String) {
                    when (module) {
                        0 -> drawerModule(moduleWidth, height - 2 * thickness, drawerCount, "$sideFa؛ کشو", "$sideEn drawer")
                        1 -> {
                            doorsForOpening(moduleWidth, height - 2 * thickness, 2, "$sideFa؛ درب کمد", "$sideEn cabinet door")
                            if (shelfCount > 0) addPart("$sideFa؛ طبقه کمد", "$sideEn cabinet shelf", moduleWidth - 2 * thickness, depth - thickness, shelfCount)
                        }
                        2 -> if (shelfCount > 0) addPart("$sideFa؛ طبقه باز", "$sideEn open shelf", moduleWidth - 2 * thickness, depth - thickness, shelfCount)
                        // Open-space module intentionally has no doors, drawers or shelves.
                    }
                }
                addModule(leftModule, "بخش چپ", "Left module")
                addModule(rightModule, "بخش راست", "Right module")
                if (productIncludeBackCheck.isChecked) {
                    addPart("پشت‌بند ماژول چپ ۳ میلی‌متری", "Left module 3 mm back", height - 2 * thickness, moduleWidth, 1, 3.0)
                    addPart("پشت‌بند ماژول راست ۳ میلی‌متری", "Right module 3 mm back", height - 2 * thickness, moduleWidth, 1, 3.0)
                }
            }
            6 -> {
                addPart("پنل اصلی دکور", "Main decor panel", height, width, 1)
                addPart("نوار عمودی دکور", "Vertical decor strip", height, depth, 2)
                if (shelfCount > 0) addPart("طبقه دکور", "Decor shelf", width, depth, shelfCount)
            }
            7 -> { // Vanity: carcass and countertop are separate pieces.
                carcassSides("بغل روشویی", "Vanity side")
                carcassTopBottom("بالا و پایین روشویی", "Vanity top/bottom")
                addShelves()
                doorsForOpening(width, height, doorCount)
                addPart("صفحه روشویی", "Vanity countertop", width, depth, 1)
                backPanel()
            }
            else -> {
                carcassSides()
                carcassTopBottom()
                addShelves()
                doorsForOpening(width, height, doorCount)
                backPanel()
            }
        }
        if (generationError || parts.isEmpty()) {
            if (showErrors && parts.isEmpty()) resultText.text = tr("از ابعاد واردشده قطعه‌ای ساخته نشد؛ ابعاد و تعداد ماژول‌ها را بررسی کنید.", "No parts could be generated; review dimensions and module settings.")
            return null
        }
        return parts
    }
