package cl.electricalc.app.ui.calculadoras

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cl.electricalc.app.R
import cl.electricalc.app.datos.Preferencias
import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.conductores.TablasConductores
import cl.electricalc.datos.ric.Verificacion

/** Estado de una pantalla de cálculo. Vive en un ViewModel para sobrevivir a la rotación. */
class EstadoCalculadora(val definicion: Calculadora, private val preferencias: Preferencias) : ViewModel() {

    var sistema by mutableStateOf(
        if (SistemaElectrico.MONOFASICA in definicion.sistemas) SistemaElectrico.MONOFASICA else definicion.sistemas.first(),
    )
        private set

    var indiceVariante by mutableIntStateOf(0)
        private set

    /** Texto de cada campo numérico, unidad elegida (símbolo) y opción elegida. */
    val textos = mutableStateMapOf<String, String>()
    private val simbolos = mutableStateMapOf<String, String>()
    private val opciones = mutableStateMapOf<String, Int>()

    var resultado by mutableStateOf<List<LineaResultado>?>(null)
        private set

    /** Verificaciones RIC del último cálculo; solo valen mientras haya [resultado]. */
    var verificaciones by mutableStateOf<List<Verificacion>>(emptyList())
        private set

    init {
        for (campo in definicion.campos) {
            when (campo) {
                is CampoNumerico -> textos[campo.id] = campo.inicial?.invoke(sistema, preferencias).orEmpty()
                is CampoOpciones -> opciones[campo.id] = campo.porDefecto
                is CampoSeccion -> elegirUnidadSeccion(if (preferencias.seccionEnAwg) CalculadoraCaida.AWG else CalculadoraCaida.MM2)
            }
        }
    }

    val variantes: List<Variante> get() = definicion.variantes(sistema)
    val variante: Variante? get() = variantes.getOrNull(indiceVariante)

    fun unidades(campo: CampoNumerico): List<Unidad> = campo.unidades(sistema)

    fun unidad(campo: CampoNumerico): Unidad {
        val lista = unidades(campo)
        return lista.firstOrNull { it.simbolo == simbolos[campo.id] }
            ?: lista.firstOrNull { it.simbolo == campo.porDefecto(preferencias) }
            ?: lista.first()
    }

    fun unidadSeccion(campo: CampoSeccion): Unidad = UNIDADES_SECCION.first { it.simbolo == simbolos[campo.id] }

    fun opcionesSeccion(campo: CampoSeccion): List<String> =
        if (unidadSeccion(campo).simbolo == CalculadoraCaida.AWG) {
            TablasConductores.CALIBRES_AWG.map { it.removeSuffix(" AWG") }
        } else {
            TablasConductores.SECCIONES_MM2.map { Formato.numero(it, 2) }
        }

    fun opcion(id: String): Int = opciones.getValue(id)

    private val unidadesActuales: Map<String, Unidad>
        get() = definicion.campos.mapNotNull { c ->
            when (c) {
                is CampoNumerico -> c.id to unidad(c)
                is CampoSeccion -> c.id to unidadSeccion(c)
                is CampoOpciones -> null
            }
        }.toMap()

    /** Campos a mostrar: primero los de la variante, en su orden, y luego el resto de los visibles. */
    val camposVisibles: List<Campo>
        get() {
            val unidades = unidadesActuales
            val visibles = definicion.campos.filter { definicion.visible(it, sistema, variante, unidades) }
            val orden = variante?.campos.orEmpty()
            return visibles.sortedBy { c -> orden.indexOf(c.id).let { if (it < 0) Int.MAX_VALUE else it } }
        }

    fun elegirSistema(nuevo: SistemaElectrico) {
        if (nuevo == sistema) return
        val anterior = sistema
        sistema = nuevo
        if (indiceVariante >= variantes.size) indiceVariante = 0
        // Se reemplaza el valor solo si el usuario no lo cambió (vacío o igual al valor por defecto anterior).
        for (campo in definicion.campos.filterIsInstance<CampoNumerico>()) {
            val inicial = campo.inicial ?: continue
            val actual = textos[campo.id].orEmpty()
            if (actual.isBlank() || actual == inicial(anterior, preferencias)) textos[campo.id] = inicial(nuevo, preferencias)
        }
        resultado = null
    }

    fun elegirVariante(indice: Int) {
        indiceVariante = indice
        resultado = null
    }

    fun escribir(id: String, texto: String) {
        textos[id] = texto
    }

    fun elegirUnidad(id: String, simbolo: String) {
        simbolos[id] = simbolo
    }

    fun elegirUnidadSeccion(simbolo: String) {
        val campo = definicion.campos.filterIsInstance<CampoSeccion>().firstOrNull() ?: return
        simbolos[campo.id] = simbolo
        opciones[campo.id] = if (simbolo == CalculadoraCaida.AWG) {
            TablasConductores.CALIBRES_AWG.indexOf("12 AWG")
        } else {
            TablasConductores.SECCIONES_MM2.indexOf(2.5)
        }
    }

    fun elegirOpcion(id: String, indice: Int) {
        opciones[id] = indice
    }

    /** Lee los campos visibles y calcula. Devuelve el mensaje de error, o null si salió bien. */
    @StringRes
    fun calcular(): Int? {
        val valores = mutableMapOf<String, Double>()
        val unidades = unidadesActuales
        for (campo in camposVisibles.filterIsInstance<CampoNumerico>()) {
            val numero = Formato.leer(textos[campo.id].orEmpty())
            if (numero == null) {
                resultado = null
                return R.string.err_faltan_datos
            }
            val base = unidad(campo).aBase(numero)
            if (base.isNaN()) {
                resultado = null
                return if (campo.id == Ids.FASE) R.string.err_cos_phi else R.string.err_valor_fuera
            }
            valores[campo.id] = base
        }
        return try {
            val entrada = Entrada(sistema, variante, valores, unidades, opciones.toMap())
            resultado = definicion.calcular(entrada)
            verificaciones = definicion.verificar(entrada)
            null
        } catch (e: ErrorCalculo) {
            resultado = null
            mensaje(e.motivo)
        } catch (_: FaltanDatos) {
            resultado = null
            R.string.err_faltan_datos
        } catch (_: IllegalArgumentException) {
            // Validaciones del motor o de las tablas (require): se informan como dato fuera de rango.
            resultado = null
            R.string.err_valor_fuera
        }
    }

    private companion object {
        val UNIDADES_SECCION = listOf(Unidad(CalculadoraCaida.MM2), Unidad(CalculadoraCaida.AWG))

        fun mensaje(motivo: MotivoError) = when (motivo) {
            MotivoError.DATOS_INSUFICIENTES -> R.string.err_datos_insuficientes
            MotivoError.NO_APLICA_EN_CONTINUA -> R.string.err_no_aplica_continua
            MotivoError.TENSION_NO_VALIDA -> R.string.err_tension
            MotivoError.CORRIENTE_NO_VALIDA -> R.string.err_corriente
            MotivoError.RESISTENCIA_NO_VALIDA -> R.string.err_resistencia
            MotivoError.IMPEDANCIA_NO_VALIDA -> R.string.err_impedancia
            MotivoError.POTENCIA_NO_VALIDA -> R.string.err_potencia
            MotivoError.COS_PHI_NO_VALIDO -> R.string.err_cos_phi
            MotivoError.VALOR_FUERA_DE_RANGO -> R.string.err_valor_fuera
        }
    }
}
