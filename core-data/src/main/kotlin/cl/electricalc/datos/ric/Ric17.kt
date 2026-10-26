package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Mantenimiento(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    @SerialName("programa_sobre_kw") val programaSobreKw: Double,
    @SerialName("usos_programa") val usosPrograma: List<String>,
    val componentes: List<String>,
    @SerialName("reglas_oro") val reglasOro: List<String>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Numeral 5.3: programa de seguridad eléctrica obligatorio. */
    fun exigePrograma(potenciaKw: Double, empalmeMt: Boolean, usoNoResidencial: Boolean): Boolean =
        usoNoResidencial && (potenciaKw > programaSobreKw || empalmeMt)
}
