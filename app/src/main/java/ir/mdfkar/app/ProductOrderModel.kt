package ir.mdfkar.app

import java.util.UUID

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

/** One order may contain any mix of products and independently entered parts. */
data class MixedProductOrder(val name: String, val lines: List<ProductOrderLine>) {
    fun isValid(): Boolean = name.isNotBlank() && lines.isNotEmpty() && lines.all { it.isValid() }
    fun allParts(): List<ProductPart> = lines.flatMap { it.expandedParts() }
    fun totalEdgeMeters(): Double = allParts().sumOf { it.edgeMeters() }
    fun sheetAreaSquareMeters(): Double = allParts().sumOf {
        it.lengthMm * it.widthMm * it.quantity
    } / 1_000_000.0

    /**
     * Sends every part in the mixed order to the app's existing cutting engine.
     * Parts are planned separately by material and thickness, because unlike materials
     * or different board thicknesses cannot share the same sheet.
     */
    fun createCuttingPlan(
        sheetWidthMm: Double = 2800.0,
        sheetHeightMm: Double = 2070.0,
        kerfMm: Double = 3.2
    ): List<MaterialCuttingPlan> {
        if (sheetWidthMm <= 0.0 || sheetHeightMm <= 0.0) return emptyList()
        return allParts()
            .filter { it.isValid() }
            .groupBy { MaterialThickness(it.stockType.trim(), it.thicknessMm) }
            .entries
            .sortedWith(compareBy({ it.key.stockType.lowercase() }, { it.key.thicknessMm }))
            .map { (group, parts) ->
                val cuttingParts = parts.map {
                    CuttingPart(
                        name = it.name,
                        width = it.widthMm,
                        height = it.lengthMm,
                        quantity = it.quantity,
                        rotate = !it.grainSensitive
                    )
                }
                val result = CuttingCalculator.packParts(
                    parts = cuttingParts,
                    sheetW = sheetWidthMm,
                    sheetH = sheetHeightMm,
                    kerf = kerfMm
                )
                MaterialCuttingPlan(
                    stockType = group.stockType,
                    thicknessMm = group.thicknessMm,
                    sheetWidthMm = sheetWidthMm,
                    sheetHeightMm = sheetHeightMm,
                    result = result,
                    edgeMeters = parts.sumOf { it.edgeMeters() },
                    totalPartAreaSquareMeters = parts.sumOf {
                        it.lengthMm * it.widthMm * it.quantity
                    } / 1_000_000.0
                )
            }
    }
}

private data class MaterialThickness(val stockType: String, val thicknessMm: Double)

data class MaterialCuttingPlan(
    val stockType: String,
    val thicknessMm: Double,
    val sheetWidthMm: Double,
    val sheetHeightMm: Double,
    val result: CuttingPackResult,
    val edgeMeters: Double,
    val totalPartAreaSquareMeters: Double
) {
    val sheetCount: Int get() = result.sheetCount
    val placements: List<SheetPlacement> get() = result.placements
    val overflowParts: List<SheetPlacement> get() = placements.filter {
        it.width > sheetWidthMm || it.height > sheetHeightMm
    }
    val totalSheetAreaSquareMeters: Double
        get() = sheetCount * sheetWidthMm * sheetHeightMm / 1_000_000.0
    val estimatedWasteSquareMeters: Double
        get() = (totalSheetAreaSquareMeters - totalPartAreaSquareMeters).coerceAtLeast(0.0)
}

/** Prices are entered in toman. Board cost is charged per complete sheet, not by area. */
data class PricingRates(
    val boardPricePerSheet: Double = 0.0,
    val edgePricePerMeter: Double = 0.0,
    val hardwareCost: Double = 0.0,
    val laborCost: Double = 0.0,
    val transportCost: Double = 0.0,
    val otherCost: Double = 0.0,
    val profitPercent: Double = 0.0
) {
    fun estimate(sheetCount: Int, edgeMeters: Double): OrderPriceEstimate {
        val safeSheetCount: Int = sheetCount.coerceAtLeast(0)
        val safeBoardPrice: Double = boardPricePerSheet.coerceAtLeast(0.0)
        val safeEdgeMeters: Double = edgeMeters.coerceAtLeast(0.0)
        val safeEdgePrice: Double = edgePricePerMeter.coerceAtLeast(0.0)
        val material: Double = safeSheetCount.toDouble() * safeBoardPrice
        val edging: Double = safeEdgeMeters * safeEdgePrice
        val hardware: Double = hardwareCost.coerceAtLeast(0.0)
        val labor: Double = laborCost.coerceAtLeast(0.0)
        val transport: Double = transportCost.coerceAtLeast(0.0)
        val other: Double = otherCost.coerceAtLeast(0.0)
        val subtotal: Double = listOf(material, edging, hardware, labor, transport, other).sum()
        val safeProfitPercent: Double = profitPercent.coerceAtLeast(0.0)
        val profit: Double = subtotal * safeProfitPercent / 100.0
        val total: Double = subtotal + profit
        return OrderPriceEstimate(material, edging, hardware, labor, transport, other, profit, total)
    }
}

data class OrderPriceEstimate(
    val board: Double,
    val edging: Double,
    val hardware: Double,
    val labor: Double,
    val transport: Double,
    val other: Double,
    val profit: Double,
    val total: Double
) {
    val subtotal: Double get() = board + edging + hardware + labor + transport + other
}
