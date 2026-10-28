package ir.mdfkar.app

/**
 * Builds the legacy cabinet form's cut list independently from the screen code.
 * All dimensions are millimetres. The back panel remains in the list for reporting,
 * but callers may keep it out of the main MDF-sheet optimizer as before.
 */
internal fun MainActivity.generateCabinetCutList(
    width: Double,
    bodyHeight: Double,
    depth: Double,
    innerWidth: Double,
    shelfCount: Int,
    doorCount: Int,
    isBaseCabinet: Boolean,
    includeBack: Boolean,
    cabinetQuantity: Int
): List<CuttingPart> {
    val parts = mutableListOf<CuttingPart>()
    parts += CuttingPart(tr("پهلو", "Side"), depth, bodyHeight, 2)
    parts += CuttingPart(tr("کف", "Bottom"), innerWidth, depth)

    if (shelfCount > 0) {
        parts += CuttingPart(tr("طبقه", "Shelf"), innerWidth, depth, shelfCount)
    }

    if (isBaseCabinet) {
        // Two top rails run from side to side and are normally about 80–100 mm deep.
        // Clamp to the actual cabinet depth so the generated part always fits the body.
        val railDepth = minOf(100.0, depth)
        parts += CuttingPart(tr("ریل بالا", "Top rail"), innerWidth, railDepth, 2)
    } else {
        parts += CuttingPart(tr("بالا", "Top"), innerWidth, depth)
    }

    if (includeBack) {
        parts += CuttingPart(tr("پشت‌بند", "Back panel"), width, bodyHeight, 1, false)
    }

    if (doorCount > 0) {
        val doorWidth = (width - 3.0) / doorCount
        val doorHeight = bodyHeight - 3.0
        if (doorWidth > 0.0 && doorHeight > 0.0) {
            parts += CuttingPart(tr("درب", "Door"), doorWidth, doorHeight, doorCount)
        }
    }

    return parts.map { it.copy(quantity = it.quantity * cabinetQuantity) }
}
