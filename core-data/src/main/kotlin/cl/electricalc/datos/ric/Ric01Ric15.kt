package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmpalmeNormalizado(val a: Double, val kw: Double, val kva: Double)

@Serializable
data class EmpalmesBt(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val monofasicos: List<EmpalmeNormalizado>,
    val trifasicos: List<EmpalmeNormalizado>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    fun lista(trifasico: Boolean) = if (trifasico) trifasicos else monofasicos

    /** Menor empalme normalizado cuya potencia cubre [demandaKw] y con protección de al menos [minimoA]. */
    fun para(demandaKw: Double, trifasico: Boolean, minimoA: Double = 0.0): EmpalmeNormalizado? =
        lista(trifasico).firstOrNull { it.kw >= demandaKw - 1e-9 && it.a >= minimoA }
}

@Serializable
data class TramoFd(@SerialName("hasta_kw") val hastaKw: Double? = null, @SerialName("fd_min") val fdMin: Double, @SerialName("fd_max") val fdMax: Double)

@Serializable
data class TipoIrve(val tipo: String, val nombre: String, val tramos: List<TramoFd>)

@Serializable
data class TramoProteccionVe(@SerialName("hasta_a") val hastaA: Double? = null, val factor: Double)

@Serializable
data class ProteccionVe(val numeral: String, val tabla: String, val tramos: List<TramoProteccionVe>)

@Serializable
data class CargaVe(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val tipos: List<TipoIrve>,
    val proteccion: ProteccionVe,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Factor de la protección de sobrecorriente para la corriente del SAVE o PCS (tabla N°15.3). */
    fun factorProteccion(inA: Double): Double = proteccion.tramos.first { it.hastaA == null || inA <= it.hastaA }.factor

    /** Demanda (kW) mínima y máxima con SGC, aplicando cada rango de la tabla N°15.1 a su tramo. */
    fun demandaConSgc(tipo: String, potenciaKw: Double): Pair<Double, Double> {
        var desde = 0.0
        var min = 0.0
        var max = 0.0
        for (t in tipos.first { it.tipo == tipo }.tramos) {
            val hasta = t.hastaKw ?: Double.POSITIVE_INFINITY
            if (potenciaKw > desde) {
                val tramo = minOf(potenciaKw, hasta) - desde
                min += tramo * t.fdMin
                max += tramo * t.fdMax
            }
            desde = hasta
        }
        return min to max
    }
}
