package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequisitosProyecto(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val partes: List<String>,
    @SerialName("memoria_obligatoria_sobre_kw") val memoriaObligatoriaSobreKw: Double,
    val calculos: List<String>,
    @SerialName("adicionales_sobre_kw") val adicionalesSobreKw: Double,
    @SerialName("calculos_adicionales") val calculosAdicionales: List<String>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Cálculos que debe justificar la memoria según la potencia declarada (numeral 6.2.3.2). */
    fun calculosPara(potenciaKw: Double): List<String> =
        calculos + if (potenciaKw > adicionalesSobreKw) calculosAdicionales else emptyList()
}
