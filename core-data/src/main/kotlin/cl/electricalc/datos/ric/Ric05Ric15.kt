package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TensionLimite(
    val numeral: String,
    @SerialName("seco_ca") val secoCa: Double, @SerialName("seco_cc") val secoCc: Double,
    @SerialName("humedo_ca") val humedoCa: Double, @SerialName("humedo_cc") val humedoCc: Double,
) {
    fun valor(humedo: Boolean, continua: Boolean) = when {
        humedo && continua -> humedoCc
        humedo -> humedoCa
        continua -> secoCc
        else -> secoCa
    }
}

@Serializable
data class EsquemaTt(val numeral: String, @SerialName("diferencial_max_ma") val diferencialMaxMa: Int)

@Serializable
data class TiempoTn(val u0: String, @SerialName("ca_s") val caS: Double, @SerialName("cc_s") val ccS: Double)

@Serializable
data class EsquemaTn(val numeral: String, val tabla: String, @SerialName("diferencial_max_ma") val diferencialMaxMa: Int, val tiempos: List<TiempoTn>)

@Serializable
data class TiempoIt(@SerialName("u0_u") val u0U: String, @SerialName("sin_neutro_s") val sinNeutroS: Double, @SerialName("con_neutro_s") val conNeutroS: Double)

@Serializable
data class EsquemaIt(val tabla: String, val tiempos: List<TiempoIt>)

@Serializable
data class ClasesDiferencial(val numeral: String, @SerialName("no_senoidal") val noSenoidal: String, @SerialName("sin_paso_por_cero") val sinPasoPorCero: String)

@Serializable
data class ProteccionContactos(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    @SerialName("tension_limite") val tensionLimite: TensionLimite,
    val tt: EsquemaTt,
    val tn: EsquemaTn,
    val it: EsquemaIt,
    val clases: ClasesDiferencial,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Resistencia máxima de la toma de tierra en TT: RA ≤ UL / Ia (numeral 8.7.7.3). */
    fun resistenciaMaximaTt(sensibilidadA: Double, humedo: Boolean, continua: Boolean = false): Double =
        tensionLimite.valor(humedo, continua) / sensibilidadA
}

@Serializable
data class ModoCarga(val modo: Int, val tipo: String, @SerialName("sensibilidad_ma") val sensibilidadMa: Int, val alternativa: String? = null, val numeral: String)

@Serializable
data class DiferencialesVe(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val modos: List<ModoCarga>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    fun modo(n: Int) = modos.first { it.modo == n }
}
