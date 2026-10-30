package ir.mdfkar.app

import java.util.UUID

/**
 * Shared, dimension-driven design model for every MDF-KAR product.
 * All dimensions and positions are millimetres.
 *
 * Coordinate convention:
 * x = left to right, y = front to back, z = bottom to top.
 * Empty openings are first-class design elements; they are not cut parts.
 * Continuous panels are independent elements and may span across openings/modules.
 */

enum class ModularElementType {
    ENCLOSED_MODULE,       // carcass/cupboard body
    DRAWER_MODULE,         // drawer stack/module
    OPEN_SHELF_MODULE,     // open shelves
    OPENING,               // intentional empty space (fridge, washer, leg space, etc.)
    APPLIANCE_OPENING,     // opening with appliance requirements
    CONTINUOUS_PANEL,      // countertop, stone top, shared top/bottom/crown
    DIVIDER,               // vertical or horizontal partition
    DOOR,
    BACK_PANEL,
    TOE_KICK,
    DECORATIVE_PANEL,
    CUSTOM
}

enum class ModularMaterialType {
    MDF, PARTICLE_BOARD, PLYWOOD, PVC, ACRYLIC, PLEXIGLASS,
    GLASS, STONE, CORIAN, METAL, NATURAL_WOOD, OTHER
}

enum class ModularOrientation { HORIZONTAL, VERTICAL, CUSTOM }

enum class OpeningPurpose {
    GENERAL, REFRIGERATOR, WASHING_MACHINE, DISHWASHER,
    OVEN, MICROWAVE, KNEE_SPACE, PIPEWORK, SERVICE_ACCESS
}

data class ModularMaterial(
    val type: ModularMaterialType = ModularMaterialType.MDF,
    val label: String = "MDF",
    val thicknessMm: Double = 16.0,
    val decor: String = "",
    val color: String = "",
    val supplier: String = "",
    val processingCenter: String = "MDF cutting"
) {
    fun isValid(): Boolean = label.isNotBlank() && thicknessMm > 0.0
}

/** A rectangular design element located in the product's overall coordinate space. */
data class ModularElement(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ModularElementType,
    val xMm: Double,
    val yMm: Double = 0.0,
    val zMm: Double,
    val widthMm: Double,
    val depthMm: Double,
    val heightMm: Double,
    val material: ModularMaterial? = null,
    val orientation: ModularOrientation = ModularOrientation.CUSTOM,
    val quantity: Int = 1,
    val parentId: String? = null,
    val notes: String = "",
    // Used by OPENING / APPLIANCE_OPENING. No board is generated for the void itself.
    val openingPurpose: OpeningPurpose = OpeningPurpose.GENERAL,
    val applianceClearanceMm: Double = 0.0,
    // Used by CONTINUOUS_PANEL to document which modules/openings it spans.
    val spansElementIds: List<String> = emptyList(),
    // Edge treatment is explicit and independent for each side.
    val edgeFront: Boolean = false,
    val edgeBack: Boolean = false,
    val edgeLeft: Boolean = false,
    val edgeRight: Boolean = false,
    val grainSensitive: Boolean = true
) {
    val rightMm: Double get() = xMm + widthMm
    val rearMm: Double get() = yMm + depthMm
    val topMm: Double get() = zMm + heightMm

    val isVoid: Boolean
        get() = type == ModularElementType.OPENING || type == ModularElementType.APPLIANCE_OPENING

    val generatesBoardPart: Boolean
        get() = !isVoid && material != null

    fun isDimensionallyValid(): Boolean =
        name.isNotBlank() && xMm >= 0.0 && yMm >= 0.0 && zMm >= 0.0 &&
            widthMm > 0.0 && depthMm > 0.0 && heightMm > 0.0 && quantity in 1..10000 &&
            applianceClearanceMm >= 0.0 && (material == null || material.isValid())

    fun contains(other: ModularElement, toleranceMm: Double = 0.0): Boolean =
        other.xMm >= xMm - toleranceMm && other.yMm >= yMm - toleranceMm &&
            other.zMm >= zMm - toleranceMm && other.rightMm <= rightMm + toleranceMm &&
            other.rearMm <= rearMm + toleranceMm && other.topMm <= topMm + toleranceMm
}

/**
 * A product is a collection of independently positioned elements.
 * The overall envelope is not assumed to be filled by cabinets.
 */
data class ModularProductDesign(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: ProductKind = ProductKind.CUSTOM,
    val widthMm: Double,
    val heightMm: Double,
    val depthMm: Double,
    val elements: List<ModularElement> = emptyList(),
    val notes: String = "",
    val revision: Int = 1
) {
    fun isValid(): Boolean = name.isNotBlank() && widthMm > 0.0 && heightMm > 0.0 &&
        depthMm > 0.0 && revision > 0 && elements.all { it.isDimensionallyValid() &&
            it.xMm + it.widthMm <= widthMm + 0.001 &&
            it.yMm + it.depthMm <= depthMm + 0.001 &&
            it.zMm + it.heightMm <= heightMm + 0.001
        } && elements.map { it.id }.distinct().size == elements.size &&
        elements.all { element -> element.parentId == null || elements.any { it.id == element.parentId } } &&
        elements.all { element -> element.spansElementIds.all { targetId -> elements.any { it.id == targetId } } }

    fun element(id: String): ModularElement? = elements.firstOrNull { it.id == id }

    fun elementsOf(type: ModularElementType): List<ModularElement> = elements.filter { it.type == type }

    fun openings(): List<ModularElement> = elements.filter { it.isVoid }

    fun continuousPanels(): List<ModularElement> =
        elements.filter { it.type == ModularElementType.CONTINUOUS_PANEL }

    fun withElement(element: ModularElement): ModularProductDesign {
        val updated = elements.filterNot { it.id == element.id } + element
        return copy(elements = updated, revision = revision + 1)
    }

    fun withoutElement(elementId: String): ModularProductDesign {
        val removedIds = linkedSetOf(elementId)
        var changed: Boolean
        do {
            changed = false
            elements.filter { it.parentId != null && it.parentId in removedIds }.forEach {
                if (removedIds.add(it.id)) changed = true
            }
        } while (changed)
        val kept = elements.filterNot { it.id in removedIds }.map { item ->
            item.copy(spansElementIds = item.spansElementIds.filterNot { it in removedIds })
        }
        return copy(elements = kept, revision = revision + 1)
    }

    /**
     * Returns human-readable validation issues. Overlap is not rejected globally:
     * a countertop intentionally overlaps/spans modules and openings.
     */
    fun validationIssues(): List<String> {
        val issues = mutableListOf<String>()
        if (name.isBlank()) issues += "نام محصول وارد نشده است."
        if (widthMm <= 0 || heightMm <= 0 || depthMm <= 0) issues += "ابعاد کلی محصول معتبر نیست."
        elements.forEach { item ->
            if (!item.isDimensionallyValid()) issues += "ابعاد یا مشخصات جزء «${item.name}» معتبر نیست."
            if (item.xMm + item.widthMm > widthMm + 0.001 ||
                item.yMm + item.depthMm > depthMm + 0.001 ||
                item.zMm + item.heightMm > heightMm + 0.001
            ) issues += "جزء «${item.name}» از محدوده ابعاد کلی محصول خارج شده است."
            if (item.parentId != null && elements.none { it.id == item.parentId })
                issues += "والد جزء «${item.name}» پیدا نشد."
            item.spansElementIds.filter { target -> elements.none { it.id == target } }
                .forEach { issues += "یکی از اجزای زیر صفحه «${item.name}» پیدا نشد." }
            if (item.isVoid && item.material != null)
                issues += "فضای خالی «${item.name}» نباید جنس ورق داشته باشد."
            if (!item.isVoid && item.type != ModularElementType.CUSTOM && item.material == null)
                issues += "جنس جزء «${item.name}» مشخص نشده است."
        }
        if (elements.map { it.id }.distinct().size != elements.size)
            issues += "شناسه تکراری برای اجزای محصول وجود دارد."
        return issues.distinct()
    }
}

/** Helpers for common real-world arrangements; all dimensions are millimetres. */
object ModularDesignExamples {
    /** Two side modules with a central empty bay and one uninterrupted top. */
    fun counterWithCentralOpening(
        name: String,
        totalWidthMm: Double,
        heightMm: Double,
        depthMm: Double,
        sideModuleWidthMm: Double,
        material: ModularMaterial = ModularMaterial(),
        topMaterial: ModularMaterial = ModularMaterial(type = ModularMaterialType.STONE, label = "سنگ", thicknessMm = 20.0, processingCenter = "Stone shop")
    ): ModularProductDesign {
        val centerWidth = totalWidthMm - 2.0 * sideModuleWidthMm
        val bodyHeight = heightMm - topMaterial.thicknessMm
        val left = ModularElement(
            name = "کمد سمت چپ", type = ModularElementType.ENCLOSED_MODULE,
            xMm = 0.0, zMm = 0.0, widthMm = sideModuleWidthMm,
            depthMm = depthMm, heightMm = bodyHeight, material = material
        )
        val gap = ModularElement(
            name = "فضای خالی وسط", type = ModularElementType.OPENING,
            xMm = sideModuleWidthMm, zMm = 0.0, widthMm = centerWidth,
            depthMm = depthMm, heightMm = bodyHeight, openingPurpose = OpeningPurpose.KNEE_SPACE,
            notes = "فضای عبور پا یا محل استفاده کاربر"
        )
        val right = ModularElement(
            name = "کمد سمت راست", type = ModularElementType.ENCLOSED_MODULE,
            xMm = sideModuleWidthMm + centerWidth, zMm = 0.0,
            widthMm = sideModuleWidthMm, depthMm = depthMm,
            heightMm = bodyHeight, material = material
        )
        val top = ModularElement(
            name = "صفحه یکپارچه روی پیشخوان", type = ModularElementType.CONTINUOUS_PANEL,
            xMm = 0.0, zMm = bodyHeight, widthMm = totalWidthMm,
            depthMm = depthMm, heightMm = topMaterial.thicknessMm,
            material = topMaterial, spansElementIds = listOf(left.id, gap.id, right.id)
        )
        return ModularProductDesign(
            name = name, kind = ProductKind.RECEPTION_COUNTER,
            widthMm = totalWidthMm, heightMm = heightMm, depthMm = depthMm,
            elements = listOf(left, gap, right, top)
        )
    }

    /** Floor units with a washing-machine bay and a single top spanning the bay. */
    fun baseRunWithWasherOpening(
        name: String,
        totalWidthMm: Double,
        heightMm: Double,
        depthMm: Double,
        washerBayWidthMm: Double = 600.0,
        material: ModularMaterial = ModularMaterial(),
        topMaterial: ModularMaterial = ModularMaterial(type = ModularMaterialType.STONE, label = "سنگ", thicknessMm = 20.0, processingCenter = "Stone shop")
    ): ModularProductDesign {
        val bodyHeight = heightMm - topMaterial.thicknessMm
        val leftWidth = (totalWidthMm - washerBayWidthMm) / 2.0
        val left = ModularElement(
            name = "کابینت سمت چپ", type = ModularElementType.ENCLOSED_MODULE,
            xMm = 0.0, zMm = 0.0, widthMm = leftWidth,
            depthMm = depthMm, heightMm = bodyHeight, material = material
        )
        val washer = ModularElement(
            name = "جای خالی لباسشویی", type = ModularElementType.APPLIANCE_OPENING,
            xMm = leftWidth, zMm = 0.0, widthMm = washerBayWidthMm,
            depthMm = depthMm, heightMm = bodyHeight,
            openingPurpose = OpeningPurpose.WASHING_MACHINE,
            applianceClearanceMm = 20.0,
            notes = "ابعاد نهایی باید با اندازه واقعی دستگاه و فضای تهویه کنترل شود."
        )
        val right = ModularElement(
            name = "کابینت سمت راست", type = ModularElementType.ENCLOSED_MODULE,
            xMm = leftWidth + washerBayWidthMm, zMm = 0.0,
            widthMm = leftWidth, depthMm = depthMm,
            heightMm = bodyHeight, material = material
        )
        val top = ModularElement(
            name = "سنگ یکپارچه روی کابینت و لباسشویی", type = ModularElementType.CONTINUOUS_PANEL,
            xMm = 0.0, zMm = bodyHeight, widthMm = totalWidthMm,
            depthMm = depthMm, heightMm = topMaterial.thicknessMm,
            material = topMaterial, spansElementIds = listOf(left.id, washer.id, right.id),
            notes = "صفحه باید مستقل از بدنه‌ها و با تکیه‌گاه/اتصال مهندسی‌شده طراحی شود."
        )
        return ModularProductDesign(
            name = name, kind = ProductKind.BASE_CABINET,
            widthMm = totalWidthMm, heightMm = heightMm, depthMm = depthMm,
            elements = listOf(left, washer, right, top)
        )
    }
}
