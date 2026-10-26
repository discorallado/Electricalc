package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VolumenBano(val volumen: Int, val ip: String, val cableado: String, val mecanismos: String, val aparatos: String)

/** Tipo de recinto húmedo: con receptáculo (bañera o plato) o ducha sin receptáculo con difusor fijo o móvil. */
enum class TipoDucha { CON_RECEPTACULO, DIFUSOR_FIJO, DIFUSOR_MOVIL }

@Serializable
data class VolumenesBano(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    @SerialName("altura_volumenes_m") val alturaVolumenes: Double,
    @SerialName("altura_max_techo_m") val alturaMaxTecho: Double,
    @SerialName("altura_volumen0_sin_receptaculo_m") val alturaVolumen0: Double,
    @SerialName("radio_difusor_movil_m") val radioMovil: Double,
    @SerialName("radio_difusor_fijo_m") val radioFijo: Double,
    @SerialName("ancho_volumen2_m") val anchoVolumen2: Double,
    @SerialName("ancho_volumen3_m") val anchoVolumen3: Double,
    @SerialName("lavamanos_m") val lavamanos: Double,
    val volumenes: List<VolumenBano>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /**
     * Volumen de un punto a [distancia] horizontal del borde de la bañera o ducha (con receptáculo) o de la
     * toma o difusor (sin receptáculo), y a [altura] sobre el suelo; null si queda fuera de los volúmenes.
     * Con receptáculo, distancia 0 es sobre la bañera (volumen 1; su interior es el volumen 0).
     */
    fun volumen(tipo: TipoDucha, distancia: Double, altura: Double, techo: Double? = null): Int? {
        require(distancia >= 0 && altura >= 0)
        val radio = when (tipo) {
            TipoDucha.CON_RECEPTACULO -> 0.0
            TipoDucha.DIFUSOR_FIJO -> radioFijo
            TipoDucha.DIFUSOR_MOVIL -> radioMovil
        }
        val tope = minOf(techo ?: alturaMaxTecho, alturaMaxTecho)
        val e = distancia - radio
        return when {
            e <= 0 -> when {
                tipo != TipoDucha.CON_RECEPTACULO && altura <= alturaVolumen0 -> 0
                altura <= alturaVolumenes -> 1
                altura <= tope -> 2
                else -> null
            }
            e <= anchoVolumen2 -> when {
                altura <= alturaVolumenes -> 2
                altura <= tope -> 3
                else -> null
            }
            e <= anchoVolumen2 + anchoVolumen3 && altura <= alturaVolumenes -> 3
            else -> null
        }
    }

    fun requisitos(volumen: Int): VolumenBano = volumenes.first { it.volumen == volumen }
}
