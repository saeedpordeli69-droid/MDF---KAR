package ir.mdfkar.app

/** One cabinet specification in an order. Dimensions are millimetres. */
data class CabinetSpec(
    val id: String,
    val name: String,
    val cabinetType: CabinetType,
    val widthMm: Double,
    val heightMm: Double,
    val depthMm: Double,
    val boardThicknessMm: Double,
    val shelfCount: Int = 0,
    val doorCount: Int = 0,
    val quantity: Int = 1,
    val plinthMm: Double = 0.0,
    val includeBack: Boolean = true,
    val backThicknessMm: Double = 3.0,
    val stockType: String = "MDF",
    val grainSensitive: Boolean = true
) {
    fun isValid(): Boolean = id.isNotBlank() && name.isNotBlank() &&
        widthMm > 0 && heightMm > 0 && depthMm > 0 && boardThicknessMm > 0 &&
        shelfCount in 0..30 && doorCount in 0..2 && quantity in 1..100 &&
        plinthMm >= 0 && backThicknessMm >= 0
}

enum class CabinetType { BASE, WALL, TALL }

/** A cutting-order line can hold cabinets with different dimensions. */
data class CabinetOrder(val cabinets: List<CabinetSpec>) {
    fun isValid(): Boolean = cabinets.isNotEmpty() && cabinets.all { it.isValid() }
}
