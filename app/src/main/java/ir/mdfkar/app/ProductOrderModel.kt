package ir.mdfkar.app

import java.util.UUID
import java.util.Locale

/** Shared product catalogue for mixed MDF jobs. Dimensions are millimetres. */
enum class ProductKind(val titleFa: String, val titleEn: String) {
    BASE_CABINET("کابینت زمینی", "Base cabinet"),
    WALL_CABINET("کابینت دیواری", "Wall cabinet"),
    TALL_CABINET("کابینت ایستاده", "Tall cabinet"),
    WARDROBE("کمد", "Wardrobe"),
    BOOKCASE("کتابخانه و قفسه", "Bookcase / shelving"),
    DESK("میز", "Desk / table"),
    SHOP_COUNTER("پیشخوان فروشگاهی", "Retail counter"),
    RECEPTION_COUNTER("پیشخوان پذیرش اداری", "Office reception counter"),
    TV_UNIT("میز تلویزیون و دکور", "TV unit / decor"),
    VANITY("کابینت روشویی", "Bathroom vanity"),
    CUSTOM("محصول سفارشی", "Custom product"),
    INDEPENDENT_PART("قطعه برش مستقل", "Independent cut part")
}

data class ProductPart(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lengthMm: Double,
    val widthMm: Double,
    val quantity: Int = 1,
    val thicknessMm: Double = 16.0,
    val stockType: String = "MDF",
    val grainSensitive: Boolean = true,
    val edgeTop: Boolean = false,
    val edgeBottom: Boolean = false,
    val edgeLeft: Boolean = false,
    val edgeRight: Boolean = false
) {
    fun isValid(): Boolean = name.isNotBlank() && lengthMm > 0 && widthMm > 0 &&
        quantity in 1..10000 && thicknessMm > 0 && stockType.isNotBlank()

    fun edgeMeters(): Double = quantity * (
        (if (edgeTop) lengthMm else 0.0) + (if (edgeBottom) lengthMm else 0.0) +
        (if (edgeLeft) widthMm else 0.0) + (if (edgeRight) widthMm else 0.0)
    ) / 1000.0

    /** Convert a shared-order part into the app's existing cutting engine format. */
    fun toCuttingPart(): CuttingPart = CuttingPart(
        name = name,
        width = widthMm,
        height = lengthMm,
        quantity = quantity,
        rotate = !grainSensitive
    )
}

data class ProductOrderLine(
    val id: String = UUID.randomUUID().toString(),
    val kind: ProductKind,
    val name: String,
    val parts: List<ProductPart>,
    val quantity: Int = 1,
    val notes: String = ""
) {
    fun isValid(): Boolean = name.isNotBlank() && quantity in 1..1000 && parts.isNotEmpty() &&
        parts.all { it.isValid() }

    fun expandedParts(): List<ProductPart> = parts.map { it.copy(quantity = it.quantity * quantity) }
}

/** One mixed order can contain any number of products and loose cut parts. */
data class MixedProductOrder(val name: String, val lines: List<ProductOrderLine>) {
    fun isValid(): Boolean = name.isNotBlank() && lines.isNotEmpty() && lines.all { it.isValid() }

    fun allParts(): List<ProductPart> = lines.flatMap { it.expandedParts() }

    fun totalEdgeMeters(): Double = allParts().sumOf { it.edgeMeters() }

    fun sheetAreaSquareMeters(): Double = allParts().sumOf {
        it.lengthMm * it.widthMm * it.quantity
    } / 1_000_000.0

    /** Group parts by material and thickness; different stock types never share a sheet. */
    fun partsByStockAndThickness(): Map<String, List<ProductPart>> = allParts()
        .groupBy { stockKey(it.stockType, it.thicknessMm) }

    /**
     * Calculate all stock groups using the existing cutting planner.
     * Sheet indices are made continuous across groups for the current layout view.
     * Parts that cannot fit are returned explicitly and are not silently omitted.
     */
    fun calculateCuttingPlan(
        sheetWidthMm: Double,
        sheetHeightMm: Double,
        kerfMm: Double
    ): MixedOrderCuttingPlan {
        if (!isValid() || sheetWidthMm <= 0.0 || sheetHeightMm <= 0.0 || kerfMm < 0.0) {
            return MixedOrderCuttingPlan(emptyList(), emptyList(), 0, 0.0, totalEdgeMeters(), false)
        }

        val allPlacements = mutableListOf<SheetPlacement>()
        val oversized = mutableListOf<ProductPart>()
        var sheetOffset = 0
        val groups = partsByStockAndThickness().toSortedMap()

        groups.values.forEach { group ->
            val fits = group.filter { part ->
                val canFitNormal = part.widthMm <= sheetWidthMm && part.lengthMm <= sheetHeightMm
                val canFitRotated = !part.grainSensitive &&
                    part.lengthMm <= sheetWidthMm && part.widthMm <= sheetHeightMm
                if (!canFitNormal && !canFitRotated) oversized += part
                canFitNormal || canFitRotated
            }
            if (fits.isNotEmpty()) {
                val packed = CuttingCalculator.packParts(
                    fits.map { it.toCuttingPart() }, sheetWidthMm, sheetHeightMm, kerfMm
                )
                allPlacements += packed.placements.map { it.copy(sheet = it.sheet + sheetOffset) }
                sheetOffset += packed.sheetCount
            }
        }

        val area = allParts().sumOf { it.lengthMm * it.widthMm * it.quantity }
        val totalSheetArea = sheetOffset * sheetWidthMm * sheetHeightMm
        val wastePercent = if (totalSheetArea > 0.0) {
            ((totalSheetArea - area) / totalSheetArea * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

        return MixedOrderCuttingPlan(
            placements = allPlacements,
            oversizedParts = oversized.distinctBy { it.id },
            sheetCount = sheetOffset,
            wastePercent = wastePercent,
            edgeMeters = totalEdgeMeters(),
            valid = oversized.isEmpty() && allPlacements.isNotEmpty()
        )
    }

    companion object {
        fun stockKey(stockType: String, thicknessMm: Double): String =
            "${stockType.trim().uppercase(Locale.ROOT)}|${"%.3f".format(Locale.US, thicknessMm)}"
    }
}

data class MixedOrderCuttingPlan(
    val placements: List<SheetPlacement>,
    val oversizedParts: List<ProductPart>,
    val sheetCount: Int,
    val wastePercent: Double,
    val edgeMeters: Double,
    val valid: Boolean
)

data class PricingRates(
    val boardPricePerSquareMeter: Double = 0.0,
    val edgePricePerMeter: Double = 0.0,
    val hardwareCost: Double = 0.0,
    val laborCost: Double = 0.0,
    val transportCost: Double = 0.0,
    val otherCost: Double = 0.0,
    val profitPercent: Double = 0.0
) {
    fun estimate(order: MixedProductOrder): PriceEstimate {
        val material = order.sheetAreaSquareMeters() * boardPricePerSquareMeter
        val edging = order.totalEdgeMeters() * edgePricePerMeter
        val subtotal = material + edging + hardwareCost + laborCost + transportCost + otherCost
        val profit = subtotal * profitPercent.coerceAtLeast(0.0) / 100.0
        return PriceEstimate(material, edging, hardwareCost, laborCost, transportCost, otherCost, profit, subtotal + profit)
    }
}

data class PriceEstimate(
    val board: Double,
    val edging: Double,
    val hardware: Double,
    val labor: Double,
    val transport: Double,
    val other: Double,
    val profit: Double,
    val total: Double
)
