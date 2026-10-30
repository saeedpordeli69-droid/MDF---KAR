package ir.mdfkar.app

/**
 * Converts a modular design into the existing order/cutting model.
 * Container elements are design-only; ModularDesignBuilder creates their
 * real manufacturing panels as child elements. Only those panels are cut.
 * All dimensions are millimetres.
 */
object ModularCutListConverter {

    data class ConversionResult(
        val orderLine: ProductOrderLine?,
        val ignoredElementNames: List<String>,
        val errors: List<String>
    ) {
        val isSuccessful: Boolean get() = orderLine != null && errors.isEmpty()
    }

    /**
     * Converts actual material-bearing panels into ProductPart rows.
     * ENCLOSED_MODULE / DRAWER_MODULE / OPEN_SHELF_MODULE are containers,
     * not physical boards, so they are deliberately excluded to prevent
     * duplicate full-size parts alongside their generated child panels.
     *
     * The dimension closest to the material thickness is treated as board
     * thickness. The remaining two dimensions form the cutting face.
     */
    fun convert(
        design: ModularProductDesign,
        quantity: Int = 1,
        notes: String = ""
    ): ConversionResult {
        if (!design.isValid()) {
            val issues = design.validationIssues().ifEmpty {
                listOf("ابعاد یا مشخصات طراحی معتبر نیست.")
            }
            return ConversionResult(null, emptyList(), issues)
        }
        if (quantity !in 1..1000) {
            return ConversionResult(null, emptyList(), listOf("تعداد محصول باید بین ۱ تا ۱۰۰۰ باشد."))
        }

        val ignored = mutableListOf<String>()
        val parts = mutableListOf<ProductPart>()
        val errors = mutableListOf<String>()

        design.elements.forEach { element ->
            when (element.type) {
                ModularElementType.ENCLOSED_MODULE,
                ModularElementType.DRAWER_MODULE,
                ModularElementType.OPEN_SHELF_MODULE -> {
                    ignored += element.name
                    return@forEach
                }
                else -> Unit
            }

            if (!element.generatesBoardPart) {
                ignored += element.name
                return@forEach
            }

            val material = element.material
            if (material == null) {
                ignored += element.name
                return@forEach
            }

            val dimensions = listOf(element.widthMm, element.depthMm, element.heightMm)
            val thicknessIndex = dimensions.indices.minByOrNull { index ->
                kotlin.math.abs(dimensions[index] - material.thicknessMm)
            } ?: 0
            val measuredThickness = dimensions[thicknessIndex]
            val tolerance = maxOf(1.0, material.thicknessMm * 0.15)
            if (kotlin.math.abs(measuredThickness - material.thicknessMm) > tolerance) {
                errors += "ضخامت قطعه «${element.name}» با ضخامت متریال (${material.thicknessMm} میلی‌متر) تطابق ندارد."
                return@forEach
            }

            val faceDimensions = dimensions.filterIndexed { index, _ -> index != thicknessIndex }
            val length = maxOf(faceDimensions[0], faceDimensions[1])
            val width = minOf(faceDimensions[0], faceDimensions[1])
            val stockName = when (material.type) {
                ModularMaterialType.MDF -> "MDF ${material.label}"
                ModularMaterialType.PARTICLE_BOARD -> "نئوپان ${material.label}"
                ModularMaterialType.PLYWOOD -> "پلای‌وود ${material.label}"
                ModularMaterialType.PVC -> "PVC ${material.label}"
                ModularMaterialType.ACRYLIC -> "اکریلیک ${material.label}"
                ModularMaterialType.PLEXIGLASS -> "پلکسی ${material.label}"
                ModularMaterialType.GLASS -> "شیشه ${material.label}"
                ModularMaterialType.STONE -> "سنگ ${material.label}"
                ModularMaterialType.CORIAN -> "کورین ${material.label}"
                ModularMaterialType.METAL -> "فلز ${material.label}"
                ModularMaterialType.NATURAL_WOOD -> "چوب طبیعی ${material.label}"
                ModularMaterialType.OTHER -> material.label
            }

            parts += ProductPart(
                id = element.id,
                name = element.name,
                lengthMm = length,
                widthMm = width,
                quantity = element.quantity,
                thicknessMm = material.thicknessMm,
                stockType = stockName,
                grainSensitive = element.grainSensitive,
                edgeTop = element.edgeFront,
                edgeBottom = element.edgeBack,
                edgeLeft = element.edgeLeft,
                edgeRight = element.edgeRight
            )
        }

        if (errors.isNotEmpty()) return ConversionResult(null, ignored, errors)
        if (parts.isEmpty()) {
            return ConversionResult(
                null,
                ignored,
                listOf("در طراحی هیچ قطعه قابل برشی با متریال مشخص وجود ندارد.")
            )
        }

        return ConversionResult(
            orderLine = ProductOrderLine(
                kind = design.kind,
                name = design.name,
                parts = parts,
                quantity = quantity,
                notes = listOf(design.notes, notes).filter { it.isNotBlank() }.joinToString("\n")
            ),
            ignoredElementNames = ignored,
            errors = emptyList()
        )
    }
}
