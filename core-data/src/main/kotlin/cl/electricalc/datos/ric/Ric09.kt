package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExencionInversa(
    @SerialName("hasta_kw") val hastaKw: Double,
    @SerialName("max_fraccion_empalme") val maxFraccionEmpalme: Double,
    @SerialName("empalme_max_kw") val empalmeMaxKw: Double? = null,
)

@Serializable
data class TiempoInversa(@SerialName("hasta_kw") val hastaKw: Double? = null, @SerialName("tiempo_s") val tiempoS: Double)

/** Protecciones exigidas a un sistema de autogeneración según su potencia y la del empalme. */
data class ProteccionesAutogeneracion(
    val diferencialMa: Int,
    val riCentralizada: Boolean,
    val potenciaInversa: Boolean,
    /** Tiempo de operación del relé de potencia inversa, si se exige. */
    val tiempoInversaS: Double?,
    val pickupKw: Double,
    val registrador: Boolean,
    val responsableTecnico: Boolean,
)

@Serializable
data class Autogeneracion(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    @SerialName("diferencial_umbral_kw") val diferencialUmbralKw: Double,
    @SerialName("diferencial_bajo_ma") val diferencialBajoMa: Int,
    @SerialName("diferencial_alto_ma") val diferencialAltoMa: Int,
    @SerialName("ri_centralizada_sobre_kw") val riCentralizadaSobreKw: Double,
    @SerialName("registrador_sobre_kw") val registradorSobreKw: Double,
    @SerialName("responsable_tecnico_sobre_kw") val responsableTecnicoSobreKw: Double,
    @SerialName("exencion_inversa") val exencionInversa: List<ExencionInversa>,
    @SerialName("rele_inversa") val releInversa: List<TiempoInversa>,
    @SerialName("rele_inversa_pickup") val releInversaPickup: Double,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    fun para(potenciaKw: Double, empalmeKw: Double, conInversor: Boolean, inyeccionCero: Boolean): ProteccionesAutogeneracion {
        require(potenciaKw > 0 && empalmeKw > 0)
        val exenta = inyeccionCero && exencionInversa.any {
            potenciaKw <= it.hastaKw && potenciaKw <= it.maxFraccionEmpalme * empalmeKw && (it.empalmeMaxKw == null || empalmeKw <= it.empalmeMaxKw)
        }
        return ProteccionesAutogeneracion(
            diferencialMa = if (potenciaKw < diferencialUmbralKw) diferencialBajoMa else diferencialAltoMa,
            riCentralizada = potenciaKw > riCentralizadaSobreKw || !conInversor,
            potenciaInversa = !exenta,
            tiempoInversaS = if (exenta) null else releInversa.first { it.hastaKw == null || potenciaKw <= it.hastaKw }.tiempoS,
            pickupKw = potenciaKw * releInversaPickup,
            registrador = potenciaKw > registradorSobreKw,
            responsableTecnico = potenciaKw > responsableTecnicoSobreKw,
        )
    }
}
