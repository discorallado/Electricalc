package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max

/** Múltiplos de In de la tabla N°13.3 para omitir la protección individual del transformador. */
@Serializable
data class ProteccionTransformador(
    val impedancia: String,
    @SerialName("primario_automatico") val primarioAutomatico: Double,
    @SerialName("primario_fusible") val primarioFusible: Double,
    @SerialName("secundario_mt_automatico") val secundarioMtAutomatico: Double,
    @SerialName("secundario_mt_fusible") val secundarioMtFusible: Double,
    @SerialName("secundario_bt") val secundarioBt: Double,
)

@Serializable
data class Transformadores(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    @SerialName("fusible_primario_max_in") val fusiblePrimarioMaxIn: Double,
    @SerialName("automatico_primario_max_in") val automaticoPrimarioMaxIn: Double,
    @SerialName("limite_impedancia_pu") val limiteImpedanciaPu: Double,
    @SerialName("tabla_13_3") val tabla133: List<ProteccionTransformador>,
    @SerialName("ventilacion_cm2_por_kva") val ventilacionCm2PorKva: Double,
    @SerialName("ventilacion_min_m2") val ventilacionMinM2: Double,
    @SerialName("renovaciones_hora") val renovacionesHora: Double,
    @SerialName("pt100_seco_sobre_kva") val pt100SecoSobreKva: Double,
    @SerialName("buchholz_aceite_sobre_kva") val buchholzAceiteSobreKva: Double,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    fun tabla133(impedanciaPu: Double): ProteccionTransformador = tabla133[if (impedanciaPu <= limiteImpedanciaPu) 0 else 1]

    /** Área libre de ventilación natural en m² (numeral 8.10). */
    fun ventilacionM2(kva: Double): Double = max(kva * ventilacionCm2PorKva / 1e4, ventilacionMinM2)

    /** Sondas o relés exigidos en edificios de uso general (9.7 y 9.8). */
    fun vigilanciaTermica(kva: Double, aceite: Boolean): Boolean =
        if (aceite) kva > buchholzAceiteSobreKva else kva > pt100SecoSobreKva
}
