package ir.mdfkar.app

import java.util.UUID

/**
 * Creates manufacturing-ready child panels for common modular elements.
 * The parent element is a design container; generated children are the actual boards.
 * All dimensions are millimetres.
 */
object ModularDesignBuilder {

    data class CabinetOptions(
        val includeBack: Boolean = true,
        val backThicknessMm: Double = 3.0,
        val shelfCount: Int = 0,
        val doorCount: Int = 0,
        val edgeFront: Boolean = true
    )

    fun addCabinet(
        design: ModularProductDesign,
        name: String,
        xMm: Double,
        yMm: Double,
        zMm: Double,
        widthMm: Double,
        depthMm: Double,
        heightMm: Double,
        material: ModularMaterial = ModularMaterial(),
        options: CabinetOptions = CabinetOptions()
    ): ModularProductDesign {
        require(name.isNotBlank())
        require(widthMm > 2 * material.thicknessMm && depthMm > 0 && heightMm > 2 * material.thicknessMm)
        require(options.shelfCount in 0..30 && options.doorCount in 0..12)
        require(options.backThicknessMm > 0)

        val t = material.thicknessMm
        val bodyHeight = heightMm
        val moduleId = UUID.randomUUID().toString()
        val module = ModularElement(
            id = moduleId, name = name, type = ModularElementType.ENCLOSED_MODULE,
            xMm = xMm, yMm = yMm, zMm = zMm,
            widthMm = widthMm, depthMm = depthMm, heightMm = bodyHeight,
            material = material
        )
        val children = mutableListOf<ModularElement>()

        fun panel(
            panelName: String, px: Double, py: Double, pz: Double,
            pw: Double, pd: Double, ph: Double,
            panelMaterial: ModularMaterial = material,
            frontEdge: Boolean = false
        ) {
            children += ModularElement(
                name = panelName, type = ModularElementType.CUSTOM,
                xMm = px, yMm = py, zMm = pz,
                widthMm = pw, depthMm = pd, heightMm = ph,
                material = panelMaterial, parentId = moduleId,
                orientation = if (ph >= pw) ModularOrientation.VERTICAL else ModularOrientation.HORIZONTAL,
                edgeFront = frontEdge,
                grainSensitive = true
            )
        }

        // Two sides, top and bottom.
        panel("$name؛ بغل چپ", xMm, yMm, zMm, t, depthMm, bodyHeight, frontEdge = options.edgeFront)
        panel("$name؛ بغل راست", xMm + widthMm - t, yMm, zMm, t, depthMm, bodyHeight, frontEdge = options.edgeFront)
        panel("$name؛ سقف", xMm + t, yMm, zMm + bodyHeight - t, widthMm - 2 * t, depthMm, t, frontEdge = options.edgeFront)
        panel("$name؛ کف", xMm + t, yMm, zMm, widthMm - 2 * t, depthMm, t, frontEdge = options.edgeFront)

        // Evenly spaced shelves inside the cabinet.
        if (options.shelfCount > 0) {
            val clearHeight = bodyHeight - 2 * t
            for (index in 1..options.shelfCount) {
                val shelfZ = zMm + t + clearHeight * index / (options.shelfCount + 1.0) - t / 2.0
                panel("$name؛ طبقه $index", xMm + t, yMm, shelfZ,
                    widthMm - 2 * t, depthMm, t, frontEdge = options.edgeFront)
            }
        }

        // Back panel is a separate material/thickness and processing stream.
        if (options.includeBack) {
            val backMaterial = material.copy(
                label = "${material.label} پشت بند",
                thicknessMm = options.backThicknessMm,
                processingCenter = "Panel cutting"
            )
            panel("$name؛ پشت بند", xMm + t, yMm + depthMm - options.backThicknessMm,
                zMm + t, widthMm - 2 * t, options.backThicknessMm,
                bodyHeight - 2 * t, backMaterial)
        }

        // Simple overlay doors; detailed hinges and door styles are added in a later stage.
        if (options.doorCount > 0) {
            val gap = 2.0
            val doorWidth = (widthMm - gap * (options.doorCount + 1)) / options.doorCount
            require(doorWidth > 0) { "عرض کابینت برای تعداد در انتخاب‌شده کافی نیست." }
            for (index in 0 until options.doorCount) {
                panel("$name؛ درب ${index + 1}",
                    xMm + gap + index * (doorWidth + gap), yMm,
                    zMm + t, doorWidth, t, bodyHeight - 2 * t,
                    frontEdge = options.edgeFront)
            }
        }

        val updated = design.copy(elements = design.elements + module + children, revision = design.revision + 1)
        require(updated.isValid()) { updated.validationIssues().joinToString("؛ ") }
        return updated
    }

    /** Adds an intentional empty bay; no board is generated for the opening. */
    fun addOpening(
        design: ModularProductDesign,
        name: String,
        xMm: Double,
        yMm: Double = 0.0,
        zMm: Double = 0.0,
        widthMm: Double,
        depthMm: Double,
        heightMm: Double,
        purpose: OpeningPurpose = OpeningPurpose.GENERAL,
        clearanceMm: Double = 0.0,
        notes: String = ""
    ): ModularProductDesign {
        val opening = ModularElement(
            name = name, type = ModularElementType.OPENING,
            xMm = xMm, yMm = yMm, zMm = zMm,
            widthMm = widthMm, depthMm = depthMm, heightMm = heightMm,
            openingPurpose = purpose, applianceClearanceMm = clearanceMm,
            notes = notes
        )
        val updated = design.copy(elements = design.elements + opening, revision = design.revision + 1)
        require(updated.isValid()) { updated.validationIssues().joinToString("؛ ") }
        return updated
    }

    /** Adds one uninterrupted panel, including over an intentional opening. */
    fun addContinuousPanel(
        design: ModularProductDesign,
        name: String,
        xMm: Double,
        yMm: Double,
        zMm: Double,
        widthMm: Double,
        depthMm: Double,
        thicknessMm: Double,
        material: ModularMaterial,
        spansElementIds: List<String> = emptyList(),
        edgeFront: Boolean = true,
        edgeBack: Boolean = false,
        edgeLeft: Boolean = false,
        edgeRight: Boolean = false,
        notes: String = ""
    ): ModularProductDesign {
        require(thicknessMm > 0)
        val panel = ModularElement(
            name = name, type = ModularElementType.CONTINUOUS_PANEL,
            xMm = xMm, yMm = yMm, zMm = zMm,
            widthMm = widthMm, depthMm = depthMm, heightMm = thicknessMm,
            material = material.copy(thicknessMm = thicknessMm),
            orientation = ModularOrientation.HORIZONTAL,
            spansElementIds = spansElementIds,
            edgeFront = edgeFront, edgeBack = edgeBack,
            edgeLeft = edgeLeft, edgeRight = edgeRight,
            notes = notes
        )
        val updated = design.copy(elements = design.elements + panel, revision = design.revision + 1)
        require(updated.isValid()) { updated.validationIssues().joinToString("؛ ") }
        return updated
    }
}
