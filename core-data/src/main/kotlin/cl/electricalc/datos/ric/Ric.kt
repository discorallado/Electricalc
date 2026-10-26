package cl.electricalc.datos.ric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Tablas y límites transcritos de los pliegos RIC (DS N°8/2019). Cada archivo en `resources/ric/`
 * trae el pliego, la versión, el numeral, la tabla y las páginas de donde sale, más su estado de
 * revisión contra el PDF oficial. La app nunca muestra un valor normativo que no esté aquí.
 */
interface FuenteRic {
    val pliego: Int
    val version: String
    val numeral: String
    val tabla: String?
    val titulo: String
    val paginas: List<Int>
    val revision: Revision
}

@Serializable
data class Revision(val estado: String, val observaciones: List<String> = emptyList()) {
    val pendiente: Boolean get() = estado != "revisada"
}

// ---------------------------------------------------------------- RIC N°03

@Serializable
data class CaidasAdmisibles(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    /** % de la tensión nominal admitido en cada alimentador, subalimentador o conductor. */
    @SerialName("por_tramo") val porTramo: Double,
    /** % admitido en el punto más desfavorable de la instalación. */
    val total: Double,
    val notas: List<String> = emptyList(),
) : FuenteRic

@Serializable
data class TramoDemanda(@SerialName("hasta_kw") val hastaKw: Double? = null, val factor: Double)

@Serializable
data class ConsumidorDemanda(val tipo: String, val tramos: List<TramoDemanda>)

@Serializable
data class FactoresDemanda(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val consumidores: List<ConsumidorDemanda>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Demanda (kW) de una potencia instalada de alumbrado, aplicando cada factor a su tramo. */
    fun demanda(tipo: String, potenciaKw: Double): Double {
        val consumidor = consumidores.first { it.tipo == tipo }
        var desde = 0.0
        var total = 0.0
        for (tramo in consumidor.tramos) {
            val hasta = tramo.hastaKw ?: Double.POSITIVE_INFINITY
            if (potenciaKw > desde) total += (minOf(potenciaKw, hasta) - desde) * tramo.factor
            desde = hasta
        }
        return total
    }
}

@Serializable
data class FactoresSimultaneidad(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    /** Factor para n = 1, 2, … (índice n − 1). */
    @SerialName("por_cantidad") val porCantidad: List<Double>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Factor para n viviendas; sobre la tabla se usa la expresión del pliego. */
    fun factor(viviendas: Int): Double {
        require(viviendas >= 1) { "Debe haber al menos una vivienda" }
        return porCantidad.getOrNull(viviendas - 1)
            ?: ((15.3 + (viviendas - porCantidad.size) * 0.5) / viviendas)
    }
}

// ---------------------------------------------------------------- RIC N°04

@Serializable
data class SeccionMinima(val uso: String, val nombre: String, val mm2: Double)

@Serializable
data class SeccionesMinimas(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val usos: List<SeccionMinima>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    fun minima(uso: String): SeccionMinima? = usos.firstOrNull { it.uso == uso }
}

@Serializable
data class ColorConductor(val conductor: String, val nombre: String, val color: String)

@Serializable
data class ColoresConductores(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val conductores: List<ColorConductor>,
    val notas: List<String> = emptyList(),
) : FuenteRic

@Serializable
data class FilaCapacidad(val mm2: Double, val awg: String? = null, val amperes: Map<String, Int?>)

@Serializable
data class BloqueCapacidad(
    @SerialName("temperatura_servicio") val temperaturaServicio: Int,
    val cables: List<String>,
    val paginas: List<Int>,
    /** Temperatura ambiente (o del suelo) de referencia de cada método. */
    @SerialName("temperatura_ambiente") val temperaturaAmbiente: Map<String, Int>,
    val filas: List<FilaCapacidad>,
) {
    val metodos: List<String> get() = temperaturaAmbiente.keys.toList()
}

@Serializable
data class CapacidadCobreAislado(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    /** Descripción de cada método de instalación (A1, A2, B1, …). */
    val metodos: Map<String, String>,
    val bloques: List<BloqueCapacidad>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    fun bloque(temperaturaServicio: Int): BloqueCapacidad? = bloques.firstOrNull { it.temperaturaServicio == temperaturaServicio }

    /** Capacidad It (A) de la tabla, o null si la sección o el método no figuran. */
    fun capacidad(temperaturaServicio: Int, metodo: String, mm2: Double): Int? =
        bloque(temperaturaServicio)?.filas?.firstOrNull { it.mm2 == mm2 }?.amperes?.get(metodo)
}

@Serializable
data class FilaUsoMovil(val mm2: Double, val awg: String? = null, val amperes: Double)

@Serializable
data class CapacidadUsoMovil(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val cables: List<String>,
    val metodo: String,
    @SerialName("temperatura_ambiente") val temperaturaAmbiente: Int,
    val filas: List<FilaUsoMovil>,
    val notas: List<String> = emptyList(),
) : FuenteRic

@Serializable
data class RangoFactor(val desde: Int, val hasta: Int? = null, val factor: Double)

@Serializable
data class FactorCantidadConductores(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val rangos: List<RangoFactor>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** fn para la cantidad de conductores activos en el ducto; hasta tres no hay corrección. */
    fun fn(conductores: Int): Double =
        rangos.firstOrNull { conductores >= it.desde && (it.hasta == null || conductores <= it.hasta) }?.factor ?: 1.0
}

@Serializable
data class ColumnaTemperatura(
    val id: String,
    @SerialName("temperatura_servicio") val temperaturaServicio: Int,
    val metodos: List<String>,
    val cables: List<String>,
)

@Serializable
data class FilaTemperatura(val desde: Int, val hasta: Int, val factores: Map<String, Double>)

@Serializable
data class FactorTemperatura(
    override val pliego: Int,
    override val version: String,
    override val numeral: String,
    override val tabla: String? = null,
    override val titulo: String,
    override val paginas: List<Int>,
    override val revision: Revision,
    val columnas: List<ColumnaTemperatura>,
    val filas: List<FilaTemperatura>,
    val notas: List<String> = emptyList(),
) : FuenteRic {
    /** Columna que corresponde a una temperatura de servicio y un método de instalación. */
    fun columna(temperaturaServicio: Int, metodo: String): ColumnaTemperatura? =
        columnas.firstOrNull { it.temperaturaServicio == temperaturaServicio && metodo in it.metodos }

    /** ft para una temperatura ambiente en °C enteros; null si está fuera de la tabla. */
    fun ft(columna: String, temperaturaC: Int): Double? =
        filas.firstOrNull { temperaturaC in it.desde..it.hasta }?.factores?.get(columna)
}

/** Acceso a las tablas transcritas. Se leen una vez desde los recursos del módulo. */
object Ric {
    private val json = Json { ignoreUnknownKeys = true }

    private inline fun <reified T> leer(ruta: String): T {
        val texto = Ric::class.java.getResourceAsStream("/ric/$ruta")
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("Falta el recurso ric/$ruta")
        return json.decodeFromString(texto)
    }

    val caidas: CaidasAdmisibles by lazy { leer("ric-03/caidas-admisibles.json") }
    val factoresDemanda: FactoresDemanda by lazy { leer("ric-03/factores-demanda-alumbrado.json") }
    val simultaneidad: FactoresSimultaneidad by lazy { leer("ric-03/factores-simultaneidad-viviendas.json") }
    val seccionesMinimas: SeccionesMinimas by lazy { leer("ric-04/secciones-minimas.json") }
    val colores: ColoresConductores by lazy { leer("ric-04/colores-conductores.json") }
    val capacidad: CapacidadCobreAislado by lazy { leer("ric-04/capacidad-cobre-aislado.json") }
    val capacidadMovil: CapacidadUsoMovil by lazy { leer("ric-04/capacidad-uso-movil.json") }
    val fn: FactorCantidadConductores by lazy { leer("ric-04/factor-cantidad-conductores.json") }
    val ft: FactorTemperatura by lazy { leer("ric-04/factor-temperatura.json") }
    val resistividad: ResistividadTerrenos by lazy { leer("ric-06/resistividad-terrenos.json") }
    val electrodos: ResistenciaElectrodos by lazy { leer("ric-06/resistencia-electrodos.json") }
    val seccionProteccion: SeccionProteccion by lazy { leer("ric-06/seccion-proteccion.json") }
    val medicionResistividad: MedicionResistividad by lazy { leer("ric-06/medicion-resistividad.json") }
    val motores: AlimentacionMotores by lazy { leer("ric-07/alimentacion-motores.json") }
    val circuitosAlumbrado: CircuitosAlumbrado by lazy { leer("ric-10/circuitos-alumbrado.json") }
    val vivienda: RequisitosVivienda by lazy { leer("ric-10/requisitos-vivienda.json") }
    val contactos: ProteccionContactos by lazy { leer("ric-05/proteccion-contactos.json") }
    val diferencialesVe: DiferencialesVe by lazy { leer("ric-15/diferenciales-ve.json") }
    val empalmes: EmpalmesBt by lazy { leer("ric-01/empalmes-bt.json") }
    val cargaVe: CargaVe by lazy { leer("ric-15/carga-ve.json") }
    val ductos: Ductos by lazy { leer("ric-04/ductos.json") }

    /** Todas las fuentes transcritas, para listarlas en la página de cada pliego. */
    val todas: List<FuenteRic>
        get() = listOf(
            caidas, factoresDemanda, simultaneidad, seccionesMinimas, colores, capacidad, capacidadMovil, fn, ft,
            resistividad, electrodos, seccionProteccion, medicionResistividad,
            motores, circuitosAlumbrado, vivienda,
            contactos, diferencialesVe,
            empalmes, cargaVe,
            ductos,
        )

    fun delPliego(numero: Int): List<FuenteRic> = todas.filter { it.pliego == numero }
}
