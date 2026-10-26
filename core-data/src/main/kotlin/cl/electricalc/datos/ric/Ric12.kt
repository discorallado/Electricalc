package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZonaPeligrosa(
    val nivel: Int,
    val gas: Int,
    val polvo: Int,
    val proteccion: String,
    val presencia: String,
    @SerialName("modos_gas") val modosGas: List<String>,
    @SerialName("modos_polvo") val modosPolvo: List<String>,
)

@Serializable
data class AreasPeligrosas(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    @SerialName("reduccion_corriente") val reduccionCorriente: Double,
    @SerialName("proteccion_cables_desde_m") val proteccionCablesDesdeM: Double,
    val zonas: List<ZonaPeligrosa>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Modos admitidos en el [nivel] (0, 1 o 2): los propios más los de las zonas más exigentes. */
    fun modos(nivel: Int, polvo: Boolean): List<String> =
        zonas.filter { it.nivel <= nivel }.flatMap { if (polvo) it.modosPolvo else it.modosGas }

    fun corrienteAdmisible(convencional: Double): Double = convencional * (1 - reduccionCorriente)
}
