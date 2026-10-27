package ir.mdfkar.app

import kotlin.math.max
import kotlin.math.min

data class CuttingPart(val name: String, val width: Double, val height: Double, val quantity: Int = 1, val rotate: Boolean = true)
data class SheetPlacement(val name: String, val x: Double, val y: Double, val width: Double, val height: Double, val rotated: Boolean, val sheet: Int)
data class CuttingPackResult(val placements: List<SheetPlacement>, val sheetCount: Int)

/** Planning-level shelf/row optimizer. Not a certified CNC optimizer. */
object CuttingCalculator {
fun packParts(parts: List<CuttingPart>, sheetW: Double, sheetH: Double, kerf: Double): CuttingPackResult {
    val expanded = mutableListOf<CuttingPart>()
    parts.forEach { p ->
        repeat(p.quantity) { expanded += p.copy(quantity = 1) }
    }
    val sorted = expanded.sortedByDescending { max(it.width, it.height) * min(it.width, it.height) }
    data class Row(var y: Double, var height: Double, var x: Double)
    val rowsBySheet = mutableListOf<MutableList<Row>>()
    val placements = mutableListOf<SheetPlacement>()

    for (part in sorted) {
        var placed = false
        for (sheetIndex in rowsBySheet.indices) {
            val rows = rowsBySheet[sheetIndex]
            for (row in rows) {
                val remaining = sheetW - row.x
                val options = mutableListOf<Triple<Double, Double, Boolean>>()
                if (part.width <= remaining && part.height <= row.height) options += Triple(part.width, part.height, false)
                if (part.rotate && part.height <= remaining && part.width <= row.height) options += Triple(part.height, part.width, true)
                val choice = options.minByOrNull { it.first * it.second }
                if (choice != null) {
                    placements += SheetPlacement(part.name, row.x, row.y, choice.first, choice.second, choice.third, sheetIndex)
                    row.x += choice.first + kerf
                    placed = true
                    break
                }
            }
            if (placed) break

            val nextY = if (rows.isEmpty()) 0.0 else rows.maxOf { it.y + it.height } + kerf
            val orientations = mutableListOf<Triple<Double, Double, Boolean>>()
            if (part.width <= sheetW && part.height <= sheetH - nextY) orientations += Triple(part.width, part.height, false)
            if (part.rotate && part.height <= sheetW && part.width <= sheetH - nextY) orientations += Triple(part.height, part.width, true)
            val choice = orientations.minByOrNull { it.second }
            if (choice != null) {
                rows += Row(nextY, choice.second, choice.first + kerf)
                placements += SheetPlacement(part.name, 0.0, nextY, choice.first, choice.second, choice.third, sheetIndex)
                placed = true
                break
            }
        }

        if (!placed) {
            val orientations = mutableListOf<Triple<Double, Double, Boolean>>()
            if (part.width <= sheetW && part.height <= sheetH) orientations += Triple(part.width, part.height, false)
            if (part.rotate && part.height <= sheetW && part.width <= sheetH) orientations += Triple(part.height, part.width, true)
            val choice = orientations.minByOrNull { it.second }
            if (choice != null) {
                val newIndex = rowsBySheet.size
                val row = Row(0.0, choice.second, choice.first + kerf)
                rowsBySheet += mutableListOf(row)
                placements += SheetPlacement(part.name, 0.0, 0.0, choice.first, choice.second, choice.third, newIndex)
            } else {
                // Keep the piece visible as an overflow placement; the output warns by dimensions.
                val newIndex = rowsBySheet.size
                rowsBySheet += mutableListOf(Row(0.0, min(part.height, sheetH), min(part.width, sheetW)))
                placements += SheetPlacement(part.name, 0.0, 0.0, part.width, part.height, false, newIndex)
            }
        }
    }
    return CuttingPackResult(placements, rowsBySheet.size)
}

}
