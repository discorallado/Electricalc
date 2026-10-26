package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.floor

// ---------------------------------------------------------------- RIC N°07: motores

@Serializable
data class SeccionMinimaMotor(val numeral: String, @SerialName("potencia_hp_sobre") val potenciaHpSobre: Double, val mm2: Double)

@Serializable
data class FactorNumeral(val numeral: String, val factor: Double)

@Serializable
data class FactorGrupo(val numeral: String, @SerialName("factor_mayor") val factorMayor: Double)

@Serializable
data class RegimenMotor(val regimen: String, val nombre: String, val factores: Map<String, Double?>)

@Serializable
data class SobrecargaMotor(
    val numeral: String,
    @SerialName("potencia_hp_sobre") val potenciaHpSobre: Double,
    @SerialName("factor_servicio_minimo") val factorServicioMinimo: Double,
    @SerialName("factor_con_servicio") val factorConServicio: Double,
    @SerialName("factor_otro") val factorOtro: Double,
)

/** Un motor de un grupo: corriente de plena carga y, si no es permanente, su régimen y período. */
data class MotorGrupo(val corriente: Double, val regimen: String? = null, val periodo: String? = null)

@Serializable
data class AlimentacionMotores(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    @SerialName("seccion_minima") val seccionMinima: SeccionMinimaMotor,
    @SerialName("factor_permanente") val factorPermanente: FactorNumeral,
    val grupo: FactorGrupo,
    @SerialName("grupo_mixto") val grupoMixto: FactorGrupo,
    val periodos: List<String>,
    val regimenes: List<RegimenMotor>,
    val sobrecarga: SobrecargaMotor,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Factor de la tabla N°7.1, o null si la celda está vacía en el pliego. */
    fun factor(regimen: String, periodo: String): Double? = regimenes.first { it.regimen == regimen }.factores[periodo]

    /** Factor de un motor: 1,25 si es permanente (5.5.2) o el de la tabla N°7.1 (5.5.3). */
    private fun factorMotor(m: MotorGrupo): Double =
        if (m.regimen == null) factorPermanente.factor
        else factor(m.regimen, requireNotNull(m.periodo) { "Falta el período de funcionamiento" })
            ?: throw IllegalArgumentException("El pliego no da factor para ${m.regimen} con período ${m.periodo}")

    /** Corriente para dimensionar el conductor de un solo motor (5.5.2 y 5.5.3). */
    fun corrienteDiseno(m: MotorGrupo): Double = m.corriente * factorMotor(m)

    /**
     * Corriente para dimensionar el alimentador de un grupo. Todos permanentes: 1,25·I mayor + Σ resto
     * (5.5.5). Con regímenes mixtos: Σ permanentes + Σ no permanentes × factor + 0,25·I mayor ×
     * (su factor si no es permanente) (5.5.6).
     */
    fun corrienteGrupo(motores: List<MotorGrupo>): Double {
        require(motores.isNotEmpty()) { "El grupo no tiene motores" }
        val mayor = motores.maxBy { it.corriente }
        if (motores.all { it.regimen == null }) {
            return grupo.factorMayor * mayor.corriente + motores.filter { it !== mayor }.sumOf { it.corriente }
        }
        val suma = motores.sumOf { if (it.regimen == null) it.corriente else it.corriente * factorMotor(it) }
        val extraMayor = grupoMixto.factorMayor * mayor.corriente * (if (mayor.regimen == null) 1.0 else factorMotor(mayor))
        return suma + extraMayor
    }

    /** Ajuste máximo de la protección de sobrecarga (5.6.1.2). */
    fun sobrecargaMaxima(corrienteNominal: Double, factorServicio: Double): Double =
        corrienteNominal * if (factorServicio >= sobrecarga.factorServicioMinimo) sobrecarga.factorConServicio else sobrecarga.factorOtro
}

// ---------------------------------------------------------------- RIC N°10: instalaciones de uso general

@Serializable
data class CircuitosAlumbrado(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    @SerialName("diferencial_max_ma") val diferencialMaxMa: Int,
    @SerialName("circuitos_por_diferencial_max") val circuitosPorDiferencialMax: Int,
    @SerialName("capacidad_adicional") val capacidadAdicional: Double,
    @SerialName("uso_capacidad_centros") val usoCapacidadCentros: Double,
    @SerialName("potencia_centro_iluminacion_w") val potenciaCentroIluminacionW: Double,
    @SerialName("potencia_enchufe_w") val potenciaEnchufeW: Double,
    val curvas: Map<String, String>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Corriente que debe cubrir la protección: potencia requerida más la capacidad adicional (5.1.4.1). */
    fun corrienteRequerida(potenciaW: Double, tensionV: Double): Double = potenciaW * (1 + capacidadAdicional) / tensionV

    /** Centros que caben: suma de potencias igual al 90 % de la capacidad nominal (5.1.4.4). */
    fun centrosMaximos(proteccionA: Double, tensionV: Double, potenciaCentroW: Double): Int =
        floor(usoCapacidadCentros * proteccionA * tensionV / potenciaCentroW + 1e-9).toInt()
}

@Serializable
data class RequisitosVivienda(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    @SerialName("empalme_proteccion_min_a") val empalmeProteccionMinA: Double,
    @SerialName("alimentador_min_mm2") val alimentadorMinMm2: Double,
    @SerialName("superficie_limite_m2") val superficieLimiteM2: Double,
    @SerialName("circuitos_min_menor") val circuitosMinMenor: Int,
    @SerialName("circuitos_min_mayor") val circuitosMinMayor: Int,
    @SerialName("circuito_cocina_min_a") val circuitoCocinaMinA: Double,
    @SerialName("circuito_horno_min_a") val circuitoHornoMinA: Double,
    @SerialName("esquema_tierra") val esquemaTierra: String,
    @SerialName("recinto_un_centro_hasta_m2") val recintoUnCentroHastaM2: Double,
    @SerialName("centros_recinto_mayor") val centrosRecintoMayor: Int,
    @SerialName("perimetro_por_enchufe_m") val perimetroPorEnchufeM: Double,
    @SerialName("enchufes_cocina") val enchufesCocina: Int,
    @SerialName("enchufes_lavadero") val enchufesLavadero: Int,
    @SerialName("potencia_enchufe_horno_min_w") val potenciaEnchufeHornoMinW: Double,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    fun circuitosMinimos(superficieM2: Double): Int = if (superficieM2 < superficieLimiteM2) circuitosMinMenor else circuitosMinMayor

    fun centrosIluminacionMinimos(superficieRecintoM2: Double): Int = if (superficieRecintoM2 <= recintoUnCentroHastaM2) 1 else centrosRecintoMayor

    /** Enchufes dobles o triples mínimos en dormitorios, living y similares: uno por cada 8 m de perímetro o fracción. */
    fun enchufesMinimos(perimetroM: Double): Int = kotlin.math.ceil(perimetroM / perimetroPorEnchufeM - 1e-9).toInt().coerceAtLeast(1)
}
