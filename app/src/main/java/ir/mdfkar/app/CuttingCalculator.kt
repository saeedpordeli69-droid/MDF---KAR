package ir.mdfkar.app

import kotlin.math.max
import kotlin.math.min

 data class CuttingPart(
    val name: String,
    val width: Double,
    val height: Double,
    val quantity: Int = 1,
    val rotate: Boolean = true
)

data class SheetPlacement(
    val name: String,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val rotated: Boolean,
    val sheet: Int
)

data class CuttingPackResult(val placements: List<SheetPlacement>, val sheetCount: Int)

/**
 * Multi-pass shelf/row optimizer for planning. It tries several part orders and
 * chooses the layout using the fewest sheets, then the lowest row-space waste.
 * This is a planning heuristic, not a certified guillotine/CNC optimizer.
 */
object CuttingCalculator {
    private data class Row(var y: Double, var height: Double, var x: Double)
    private data class Layout(val result: CuttingPackResult, val rowWaste: Double, val totalRowHeight: Double)
    private data class Fit(
        val sheet: Int,
        val row: Row,
        val width: Double,
        val height: Double,
        val rotated: Boolean,
        val heightSlack: Double,
        val widthSlack: Double
    )
    private data class NewRowFit(
        val sheet: Int,
        val y: Double,
        val width: Double,
        val height: Double,
        val rotated: Boolean,
        val verticalSlack: Double
    )

    fun packParts(parts: List<CuttingPart>, sheetW: Double, sheetH: Double, kerf: Double): CuttingPackResult {
        if (sheetW <= 0.0 || sheetH <= 0.0) return CuttingPackResult(emptyList(), 0)
        val expanded = mutableListOf<CuttingPart>()
        parts.filter { it.quantity > 0 && it.width > 0.0 && it.height > 0.0 }.forEach { part ->
            repeat(part.quantity) { expanded += part.copy(quantity = 1) }
        }
        if (expanded.isEmpty()) return CuttingPackResult(emptyList(), 0)

        // Different orderings often produce different shelf layouts. Try each and
        // retain the best valid result instead of relying on one area-only sort.
        val orders = listOf(
            expanded.sortedWith(compareByDescending<CuttingPart> { area(it) }.thenByDescending { max(it.width, it.height) }),
            expanded.sortedWith(compareByDescending<CuttingPart> { max(it.width, it.height) }.thenByDescending { area(it) }),
            expanded.sortedWith(compareByDescending<CuttingPart> { max(it.width, it.height) }.thenByDescending { min(it.width, it.height) }),
            expanded.sortedWith(compareByDescending<CuttingPart> { it.height }.thenByDescending { it.width }),
            expanded.sortedWith(compareByDescending<CuttingPart> { it.width }.thenByDescending { it.height }),
            expanded.sortedWith(compareByDescending<CuttingPart> { it.width + it.height }.thenByDescending { area(it) })
        )

        var best: Layout? = null
        for (order in orders) {
            val candidate = packInOrder(order, sheetW, sheetH, max(0.0, kerf))
            if (best == null || isBetter(candidate, best!!)) best = candidate
        }
        return best!!.result
    }

    private fun area(part: CuttingPart): Double = part.width * part.height

    private fun isBetter(candidate: Layout, current: Layout): Boolean {
        if (candidate.result.sheetCount != current.result.sheetCount) {
            return candidate.result.sheetCount < current.result.sheetCount
        }
        val eps = 0.0001
        if (candidate.rowWaste < current.rowWaste - eps) return true
        if (candidate.rowWaste > current.rowWaste + eps) return false
        return candidate.totalRowHeight < current.totalRowHeight - eps
    }

    private fun packInOrder(parts: List<CuttingPart>, sheetW: Double, sheetH: Double, kerf: Double): Layout {
        val rowsBySheet = mutableListOf<MutableList<Row>>()
        val placements = mutableListOf<SheetPlacement>()

        for (part in parts) {
            val orientations = mutableListOf<Triple<Double, Double, Boolean>>()
            if (part.width <= sheetW && part.height <= sheetH) {
                orientations += Triple(part.width, part.height, false)
            }
            if (part.rotate && part.height <= sheetW && part.width <= sheetH &&
                (part.width != part.height || orientations.isEmpty())) {
                orientations += Triple(part.height, part.width, true)
            }

            // A piece that cannot fit even on a fresh sheet is kept visible as an
            // overflow marker; MainActivity also warns and blocks this situation.
            if (orientations.isEmpty()) {
                val newIndex = rowsBySheet.size
                rowsBySheet += mutableListOf(Row(0.0, min(part.height, sheetH), min(part.width, sheetW)))
                placements += SheetPlacement(part.name, 0.0, 0.0, part.width, part.height, false, newIndex)
                continue
            }

            // 1) Best-fit existing row across every sheet, not just the first sheet.
            var bestFit: Fit? = null
            for (sheetIndex in rowsBySheet.indices) {
                for (row in rowsBySheet[sheetIndex]) {
                    val remaining = sheetW - row.x
                    for ((w, h, rotated) in orientations) {
                        if (w <= remaining && h <= row.height) {
                            val fit = Fit(
                                sheetIndex, row, w, h, rotated,
                                row.height - h,
                                remaining - w
                            )
                            if (bestFit == null || compareFit(fit, bestFit!!) < 0) bestFit = fit
                        }
                    }
                }
            }
            if (bestFit != null) {
                val fit = bestFit!!
                placements += SheetPlacement(part.name, fit.row.x, fit.row.y, fit.width, fit.height, fit.rotated, fit.sheet)
                fit.row.x += fit.width + kerf
                continue
            }

            // 2) Otherwise choose the tightest new row on any existing sheet.
            var bestNewRow: NewRowFit? = null
            for (sheetIndex in rowsBySheet.indices) {
                val rows = rowsBySheet[sheetIndex]
                val nextY = if (rows.isEmpty()) 0.0 else rows.maxOf { it.y + it.height } + kerf
                for ((w, h, rotated) in orientations) {
                    if (w <= sheetW && nextY + h <= sheetH) {
                        val fit = NewRowFit(sheetIndex, nextY, w, h, rotated, sheetH - (nextY + h))
                        if (bestNewRow == null || compareNewRow(fit, bestNewRow!!) < 0) bestNewRow = fit
                    }
                }
            }
            if (bestNewRow != null) {
                val fit = bestNewRow!!
                val row = Row(fit.y, fit.height, fit.width + kerf)
                rowsBySheet[fit.sheet] += row
                placements += SheetPlacement(part.name, 0.0, fit.y, fit.width, fit.height, fit.rotated, fit.sheet)
                continue
            }

            // 3) No existing sheet can fit it: start a new sheet.
            val choice = orientations.minWithOrNull(
                compareBy<Triple<Double, Double, Boolean>> { sheetH - it.second }
                    .thenBy { sheetW - it.first }
            )!!
            val newIndex = rowsBySheet.size
            rowsBySheet += mutableListOf(Row(0.0, choice.second, choice.first + kerf))
            placements += SheetPlacement(part.name, 0.0, 0.0, choice.first, choice.second, choice.third, newIndex)
        }

        val totalRowHeight = rowsBySheet.sumOf { rows -> rows.sumOf { it.height } }
        val usedPieceArea = placements.sumOf { it.width * it.height }
        val occupiedRowArea = totalRowHeight * sheetW
        val rowWaste = max(0.0, occupiedRowArea - usedPieceArea)
        return Layout(CuttingPackResult(placements, rowsBySheet.size), rowWaste, totalRowHeight)
    }

    private fun compareFit(a: Fit, b: Fit): Int {
        val heightCompare = a.heightSlack.compareTo(b.heightSlack)
        if (heightCompare != 0) return heightCompare
        val widthCompare = a.widthSlack.compareTo(b.widthSlack)
        if (widthCompare != 0) return widthCompare
        return a.sheet.compareTo(b.sheet)
    }

    private fun compareNewRow(a: NewRowFit, b: NewRowFit): Int {
        val verticalCompare = a.verticalSlack.compareTo(b.verticalSlack)
        if (verticalCompare != 0) return verticalCompare
        val heightCompare = b.height.compareTo(a.height) // prefer taller rows when vertical slack ties
        if (heightCompare != 0) return heightCompare
        return a.sheet.compareTo(b.sheet)
    }
}
