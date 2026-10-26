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
