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
 * Multi-pass MaxRects-style planner.
 *
 * Unlike a shelf-only planner, it tracks rectangular free areas left between parts.
 * It tries multiple orderings and prefers the layout with the fewest sheets first.
 * This is a planning heuristic, not a certified CNC / guillotine optimizer.
 */
object CuttingCalculator {
    private data class Rect(val x: Double, val y: Double, val w: Double, val h: Double)
    private data class Candidate(
        val sheet: Int,
        val free: Rect,
        val w: Double,
        val h: Double,
        val rotated: Boolean,
        val shortSide: Double,
        val longSide: Double,
        val areaWaste: Double
    )
    private data class Layout(val result: CuttingPackResult, val freeArea: Double)

    fun packParts(
        parts: List<CuttingPart>,
        sheetW: Double,
        sheetH: Double,
        kerf: Double
    ): CuttingPackResult {
        if (sheetW <= 0.0 || sheetH <= 0.0) return CuttingPackResult(emptyList(), 0)

        val expanded = mutableListOf<CuttingPart>()
        parts.filter { it.quantity > 0 && it.width > 0.0 && it.height > 0.0 }.forEach { part ->
            repeat(part.quantity) { expanded += part.copy(quantity = 1) }
        }
        if (expanded.isEmpty()) return CuttingPackResult(emptyList(), 0)

        // Reject no part silently: oversized pieces remain visible as overflow placements,
        // so the calling screen can report them to the user.
        val orders = listOf(
            expanded.sortedWith(compareByDescending<CuttingPart> { area(it) }.thenByDescending { max(it.width, it.height) }),
            expanded.sortedWith(compareByDescending<CuttingPart> { max(it.width, it.height) }.thenByDescending { min(it.width, it.height) }),
            expanded.sortedWith(compareByDescending<CuttingPart> { min(it.width, it.height) }.thenByDescending { area(it) }),
            expanded.sortedWith(compareByDescending<CuttingPart> { it.width }.thenByDescending { it.height }),
            expanded.sortedWith(compareByDescending<CuttingPart> { it.height }.thenByDescending { it.width }),
            expanded.sortedWith(compareByDescending<CuttingPart> { it.width + it.height }.thenByDescending { area(it) }),
            expanded.sortedWith(compareBy<CuttingPart> { min(it.width, it.height) }.thenByDescending { area(it) })
        )

        var best: Layout? = null
        for (order in orders) {
            val candidate = packInOrder(order, sheetW, sheetH, max(0.0, kerf))
            if (best == null || isBetter(candidate, best!!)) best = candidate
        }
        return best!!.result
    }

    private fun area(p: CuttingPart) = p.width * p.height

    private fun isBetter(a: Layout, b: Layout): Boolean {
        if (a.result.sheetCount != b.result.sheetCount) {
            return a.result.sheetCount < b.result.sheetCount
        }
        // With sheet count tied, prefer less remaining free area, then fewer overflow parts.
        val eps = 0.0001
        if (a.freeArea < b.freeArea - eps) return true
        if (a.freeArea > b.freeArea + eps) return false
        return a.result.placements.count { it.width > 0 && it.height > 0 } >
            b.result.placements.count { it.width > 0 && it.height > 0 }
    }

    private fun packInOrder(
        parts: List<CuttingPart>,
        sheetW: Double,
        sheetH: Double,
        kerf: Double
    ): Layout {
        val freeBySheet = mutableListOf<MutableList<Rect>>()
        val placements = mutableListOf<SheetPlacement>()
        val sheetArea = sheetW * sheetH

        for (part in parts) {
            val orientations = mutableListOf<Triple<Double, Double, Boolean>>()
            if (part.width <= sheetW && part.height <= sheetH) {
                orientations += Triple(part.width, part.height, false)
            }
            if (part.rotate && part.height <= sheetW && part.width <= sheetH &&
                (part.width != part.height || orientations.isEmpty())) {
                orientations += Triple(part.height, part.width, true)
            }

            if (orientations.isEmpty()) {
                val index = freeBySheet.size
                freeBySheet.add(mutableListOf())
                placements += SheetPlacement(part.name, 0.0, 0.0, part.width, part.height, false, index)
                continue
            }

            var best: Candidate? = null
            // Critical rule: search every existing sheet before allocating another one.
            for (sheet in freeBySheet.indices) {
                for (free in freeBySheet[sheet]) {
                    for ((w, h, rotated) in orientations) {
                        // Reserve kerf on the right/bottom of each part unless it touches the sheet edge.
                        val reserveW = w + if (w < sheetW) kerf else 0.0
                        val reserveH = h + if (h < sheetH) kerf else 0.0
                        if (reserveW <= free.w && reserveH <= free.h) {
                            val dw = free.w - reserveW
                            val dh = free.h - reserveH
                            val candidate = Candidate(
                                sheet, free, w, h, rotated,
                                min(dw, dh), max(dw, dh), free.w * free.h - w * h
                            )
                            if (best == null || compareCandidate(candidate, best!!) < 0) best = candidate
                        }
                    }
                }
            }

            if (best == null) {
                // Only now create a new sheet, and initialize its full free rectangle.
                val index = freeBySheet.size
                freeBySheet.add(mutableListOf(Rect(0.0, 0.0, sheetW, sheetH)))
                for ((w, h, rotated) in orientations) {
                    val reserveW = w + if (w < sheetW) kerf else 0.0
                    val reserveH = h + if (h < sheetH) kerf else 0.0
                    val dw = sheetW - reserveW
                    val dh = sheetH - reserveH
                    val candidate = Candidate(
                        index, Rect(0.0, 0.0, sheetW, sheetH), w, h, rotated,
                        min(dw, dh), max(dw, dh), sheetArea - w * h
                    )
                    if (best == null || compareCandidate(candidate, best!!) < 0) best = candidate
                }
            }

            val chosen = best!!
            placements += SheetPlacement(
                part.name, chosen.free.x, chosen.free.y,
                chosen.w, chosen.h, chosen.rotated, chosen.sheet
            )
            val reserveW = chosen.w + if (chosen.w < sheetW) kerf else 0.0
            val reserveH = chosen.h + if (chosen.h < sheetH) kerf else 0.0
            val used = Rect(chosen.free.x, chosen.free.y, reserveW, reserveH)
            val freeRects = freeBySheet[chosen.sheet]
            splitFreeRects(freeRects, used)
            pruneFreeRects(freeRects)
        }

        val sheetCount = freeBySheet.size
        val placedArea = placements.sumOf { it.width * it.height }
        val freeArea = max(0.0, sheetCount * sheetArea - placedArea)
        return Layout(CuttingPackResult(placements, sheetCount), freeArea)
    }

    private fun compareCandidate(a: Candidate, b: Candidate): Int {
        val short = a.shortSide.compareTo(b.shortSide)
        if (short != 0) return short
        val long = a.longSide.compareTo(b.longSide)
        if (long != 0) return long
        val area = a.areaWaste.compareTo(b.areaWaste)
        if (area != 0) return area
        // Prefer using the earliest sheet when the fit quality is otherwise identical.
        return a.sheet.compareTo(b.sheet)
    }

    private fun splitFreeRects(freeRects: MutableList<Rect>, used: Rect) {
        val updated = mutableListOf<Rect>()
        val iterator = freeRects.toList()
        for (free in iterator) {
            if (!intersects(free, used)) continue
            freeRects.remove(free)

            if (used.x > free.x && used.x < free.x + free.w) {
                updated += Rect(free.x, free.y, used.x - free.x, free.h)
            }
            if (used.x + used.w < free.x + free.w) {
                updated += Rect(used.x + used.w, free.y, free.x + free.w - (used.x + used.w), free.h)
            }
            if (used.y > free.y && used.y < free.y + free.h) {
                updated += Rect(free.x, free.y, free.w, used.y - free.y)
            }
            if (used.y + used.h < free.y + free.h) {
                updated += Rect(free.x, used.y + used.h, free.w, free.y + free.h - (used.y + used.h))
            }
        }
        freeRects.addAll(updated.filter { it.w > 0.0001 && it.h > 0.0001 })
    }

    private fun intersects(a: Rect, b: Rect): Boolean {
        return b.x < a.x + a.w && b.x + b.w > a.x &&
            b.y < a.y + a.h && b.y + b.h > a.y
    }

    private fun pruneFreeRects(rects: MutableList<Rect>) {
        var i = 0
        while (i < rects.size) {
            var removedI = false
            var j = i + 1
            while (j < rects.size) {
                val a = rects[i]
                val b = rects[j]
                if (contains(a, b)) {
                    rects.removeAt(j)
                } else if (contains(b, a)) {
                    rects.removeAt(i)
                    removedI = true
                    break
                } else {
                    j++
                }
            }
            if (!removedI) i++ 
        }
    }

    private fun contains(a: Rect, b: Rect): Boolean =
        b.x >= a.x - 0.0001 && b.y >= a.y - 0.0001 &&
            b.x + b.w <= a.x + a.w + 0.0001 &&
            b.y + b.h <= a.y + a.h + 0.0001
}
