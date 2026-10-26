package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.floor

@Serializable
data class Ocupacion(
    val uno: Double,
    val dos: Double,
    @SerialName("tres_o_mas") val tresOMas: Double,
    @SerialName("union_gabinetes") val unionGabinetes: Double,
    @SerialName("union_largo_max_m") val unionLargoMaxM: Double,
    @SerialName("redondeo_decimal") val redondeoDecimal: Double,
)

@Serializable
data class Bandejas(val numeral: String, val porcentaje: Double)

@Serializable
data class FilaCable(
    val seccion: String,
    @SerialName("diametro_mm") val diametroMm: Double,
    @SerialName("area_mm2") val areaMm2: Double,
    /** Diámetro nominal del ducto para 1 a 5 conductores. */
    @SerialName("ductos_mm") val ductosMm: List<Int>,
)

@Serializable
data class TablaCables(val tabla: String, val titulo: String, val unidad: String, val cables: List<String>, val filas: List<FilaCable>)

@Serializable
data class Ductos(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val ocupacion: Ocupacion,
    val bandejas: Bandejas,
    @SerialName("largo_max_m") val largoMaxM: Double,
    val tablas: List<TablaCables>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** % de la sección de la tubería que pueden ocupar [conductores] (tabla N°4.21). */
    fun porcentaje(conductores: Int): Double = when (conductores) {
        1 -> ocupacion.uno
        2 -> ocupacion.dos
        else -> ocupacion.tresOMas
    }

    /**
     * Conductores iguales que caben en una tubería de diámetro interior [diametroMm]. Los decimales
     * se aproximan al entero superior solo desde 0,8 (condición de la tabla N°4.21).
     */
    fun cantidadMaxima(diametroMm: Double, areaCableMm2: Double): Int {
        val area = PI / 4 * diametroMm * diametroMm
        // Se prueba de mayor a menor: con más conductores el porcentaje admitido baja.
        for (n in 200 downTo 1) {
            val x = area * porcentaje(n) / 100 / areaCableMm2
            val entero = if (x - floor(x) >= ocupacion.redondeoDecimal - 1e-9) floor(x) + 1 else floor(x)
            if (entero >= n) return n
        }
        return 0
    }

    /** Conductores iguales en una bandeja o moldura de área útil [areaUtilMm2] (40 %). */
    fun cantidadEnBandeja(areaUtilMm2: Double, areaCableMm2: Double): Int =
        floor(areaUtilMm2 * bandejas.porcentaje / 100 / areaCableMm2 + 1e-9).toInt()
}
