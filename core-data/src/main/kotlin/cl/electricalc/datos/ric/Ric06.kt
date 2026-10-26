package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------- RIC N°06: puesta a tierra

@Serializable
data class ResistividadTerreno(val terreno: String, val minimo: Double? = null, val maximo: Double, val texto: String? = null)

@Serializable
data class ResistividadMedia(val terreno: String, val valor: Double)

@Serializable
data class ValoresMedios(val tabla: String, val titulo: String, val terrenos: List<ResistividadMedia>)

@Serializable
data class ResistividadTerrenos(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val terrenos: List<ResistividadTerreno>,
    @SerialName("valores_medios") val valoresMedios: ValoresMedios,
    val notas: List<String> = emptyList(),
) : FuenteRic

@Serializable
data class FormulaElectrodo(
    val electrodo: String,
    val nombre: String,
    val formula: String,
    /** R = coeficiente × ρ / variable (perímetro de la placa o longitud). */
    val coeficiente: Double,
    val variable: String,
)

@Serializable
data class ResistenciaElectrodos(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val electrodos: List<FormulaElectrodo>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    fun formula(electrodo: String): FormulaElectrodo = electrodos.first { it.electrodo == electrodo }

    /** Resistencia estimada (Ω) de un electrodo en un terreno de resistividad [rho] (Ω·m). */
    fun resistencia(electrodo: String, rho: Double, medida: Double): Double {
        require(rho > 0 && medida > 0) { "Resistividad y medida deben ser positivas" }
        return formula(electrodo).coeficiente * rho / medida
    }
}

@Serializable
data class TramoProteccion(@SerialName("fase_hasta") val faseHasta: Double? = null, val proteccion: String)

@Serializable
data class SeccionProteccion(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val tramos: List<TramoProteccion>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Sección mínima (mm²) del conductor de protección de cobre para una fase de [fase] mm². */
    fun minima(fase: Double): Double {
        val tramo = tramos.first { it.faseHasta == null || fase <= it.faseHasta }
        return when (tramo.proteccion) {
            "S" -> fase
            "S/2" -> fase / 2
            else -> tramo.proteccion.toDouble()
        }
    }
}

@Serializable
data class MedicionResistividad(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val metodos: List<String>,
    @SerialName("ala_maxima_m") val alaMaximaM: Double,
    @SerialName("ala_reducida_m") val alaReducidaM: Double,
    @SerialName("ala_excepcional_m") val alaExcepcionalM: Double,
    val notas: List<String> = emptyList(),
) : FuenteRic
