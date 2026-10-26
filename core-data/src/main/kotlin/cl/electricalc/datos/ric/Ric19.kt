package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AislamientoMinimo(
    val circuito: String,
    val nombre: String,
    @SerialName("ensayo_v") val ensayoV: Int,
    @SerialName("minimo_mohm") val minimoMohm: Double,
)

@Serializable
data class PuestaEnServicio(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val ensayos: List<String>,
    @SerialName("mediciones_aislamiento") val medicionesAislamiento: List<String>,
    val aislamiento: List<AislamientoMinimo>,
    val notas: List<String> = emptyList(),
) : FuenteRic

@Serializable
data class FilaBucle(
    @SerialName("in_a") val inA: Int,
    @SerialName("zs_b") val zsB: Double,
    @SerialName("zs_c") val zsC: Double,
    @SerialName("zs_d") val zsD: Double,
) {
    fun zs(curva: Char): Double = when (curva) {
        'B' -> zsB
        'C' -> zsC
        'D' -> zsD
        else -> throw IllegalArgumentException("Curva $curva")
    }
}

@Serializable
data class ImpedanciaBucle(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    @SerialName("tension_fase_neutro_v") val tension: Double,
    @SerialName("multiplicador_disparo") val multiplicador: Map<String, Int>,
    @SerialName("tt_max_ohm") val ttMaxOhm: Double,
    @SerialName("factor_ipcc") val factorIpcc: Double,
    val filas: List<FilaBucle>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    fun maxima(inA: Int, curva: Char): Double = filas.first { it.inA == inA }.zs(curva)

    fun ipcc(zs: Double): Double = tension * factorIpcc / zs
}
