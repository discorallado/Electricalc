package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.ceil

@Serializable
data class EspacioTrabajo(val condicion: Int, val descripcion: String, val m: Double)

@Serializable
data class Tableros(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val ampliacion: Double,
    @SerialName("bandeja_ocupacion_max") val bandejaOcupacionMax: Double,
    @SerialName("bandeja_desde_circuitos") val bandejaDesdeCircuitos: Int,
    @SerialName("espacios_trabajo") val espaciosTrabajo: List<EspacioTrabajo>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Módulos DIN que debe admitir el gabinete: los usados más la ampliación del 25 % (6.1.16.3). */
    fun modulosConReserva(modulos: Int): Int = ceil(modulos * (1 + ampliacion) - 1e-9).toInt()

    fun requiereBandeja(circuitos: Int): Boolean = circuitos >= bandejaDesdeCircuitos
}
