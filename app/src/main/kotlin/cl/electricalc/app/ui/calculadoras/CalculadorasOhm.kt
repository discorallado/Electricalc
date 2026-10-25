package cl.electricalc.app.ui.calculadoras

import androidx.annotation.StringRes
import cl.electricalc.app.R
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.ohm.LeyOhm
import cl.electricalc.calc.ohm.Magnitud
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Las ocho pantallas de ley de Ohm y potencias. Las combinaciones de datos conocidos son las de la
 * app de referencia; el cálculo lo hace [LeyOhm].
 */
object CalculadorasOhm {
    private val todos = listOf(
        Campos.tension, Campos.corriente, Campos.potencia, Campos.potenciaActiva, Campos.potenciaAparente,
        Campos.potenciaReactiva, Campos.resistencia, Campos.impedancia, Campos.fase,
    ).associateBy { it.id }

    private val ALTERNA = listOf(SistemaElectrico.MONOFASICA, SistemaElectrico.BIFASICA, SistemaElectrico.TRIFASICA)
    private val TODOS = listOf(SistemaElectrico.CONTINUA) + ALTERNA

    private const val U = Ids.TENSION
    private const val I = Ids.CORRIENTE
    private const val RES = Ids.RESISTENCIA
    private const val Z = Ids.IMPEDANCIA
    private const val POT = Ids.POTENCIA
    private const val P = Ids.POTENCIA_ACTIVA
    private const val S = Ids.POTENCIA_APARENTE
    private const val Q = Ids.POTENCIA_REACTIVA

    private fun v(vararg ids: String) = Variante(ids.toList())

    val corriente = ohm(
        id = "CalcoloIntensita", salida = Magnitud.CORRIENTE, sistemas = TODOS,
        cc = listOf(v(U, POT), v(U, RES), v(POT, RES)),
        ca = listOf(v(U, POT), v(U, Z), v(U, RES), v(POT, Z), v(POT, RES)),
        intro = R.string.intro_corriente, etiqueta = R.string.res_corriente, simbolo = "A",
    )

    val tension = ohm(
        id = "CalcoloTensione", salida = Magnitud.TENSION, sistemas = TODOS,
        cc = listOf(v(I, POT), v(I, RES), v(POT, RES)),
        ca = listOf(v(I, POT), v(I, Z), v(I, RES), v(POT, Z), v(POT, RES)),
        intro = R.string.intro_tension, etiqueta = R.string.res_tension, simbolo = "V",
    )

    val potenciaActiva = ohm(
        id = "CalcoloPotenzaAttiva", salida = Magnitud.POTENCIA_ACTIVA, sistemas = TODOS,
        cc = listOf(v(U, I), v(U, RES), v(I, RES)),
        ca = listOf(v(U, I), v(U, Z), v(U, RES), v(I, Z), v(I, RES), v(S, Q), v(S), v(Q)),
        intro = R.string.intro_potencia_activa, etiqueta = R.string.res_potencia_activa, simbolo = "W",
    )

    val potenciaAparente = ohm(
        id = "CalcoloPotenzaApparente", salida = Magnitud.POTENCIA_APARENTE, sistemas = TODOS,
        cc = listOf(v(U, I)),
        ca = listOf(v(U, I), v(P, Q), v(I, Z), v(I, RES), v(U, Z), v(U, RES), v(P), v(Q)),
        intro = R.string.intro_potencia_aparente, etiqueta = R.string.res_potencia_aparente, simbolo = "VA",
    )

    val potenciaReactiva = ohm(
        id = "CalcoloPotenzaReattiva", salida = Magnitud.POTENCIA_REACTIVA, sistemas = ALTERNA,
        cc = emptyList(),
        ca = listOf(v(U, I), v(P, S), v(I, Z), v(I, RES), v(U, Z), v(U, RES), v(P), v(S)),
        intro = R.string.intro_potencia_reactiva, etiqueta = R.string.res_potencia_reactiva, simbolo = "var",
    )

    val factorPotencia = ohm(
        id = "CalcoloFattorePotenza", salida = null, sistemas = ALTERNA,
        cc = emptyList(),
        ca = listOf(v(U, I, P), v(U, I, Q), v(U, P, Z), v(I, P, Z), v(P, S), v(P, Q), v(S, Q), v(RES, Z)),
        intro = R.string.intro_factor_potencia, etiqueta = R.string.res_factor_potencia, simbolo = "",
    )

    val resistencia = ohm(
        id = "CalcoloResistenza", salida = Magnitud.RESISTENCIA, sistemas = TODOS,
        cc = listOf(v(U, I), v(U, POT), v(I, POT)),
        ca = listOf(v(U, I), v(U, POT), v(I, POT), v(Z)),
        intro = R.string.intro_resistencia, etiqueta = R.string.res_resistencia, simbolo = "Ω",
    )

    val impedancia = ohm(
        id = "CalcoloImpedenza", salida = Magnitud.IMPEDANCIA, sistemas = ALTERNA,
        cc = emptyList(),
        ca = listOf(v(U, I), v(U, POT), v(I, POT), v(RES)),
        intro = R.string.intro_impedancia, etiqueta = R.string.res_impedancia, simbolo = "Ω",
    )

    val todas = listOf(
        corriente, tension, potenciaActiva, potenciaAparente, potenciaReactiva, factorPotencia, resistencia, impedancia,
    )

    /** Magnitud que representa un campo: la propia o la de la unidad elegida (potencia genérica). */
    private fun magnitud(id: String, unidad: Unidad?): Magnitud? =
        (todos.getValue(id) as CampoNumerico).magnitud ?: unidad?.magnitud

    /**
     * El cosφ se pide solo si cambia el resultado. Se prueba con un circuito de referencia: si al
     * resolverlo con otro cosφ se obtiene el mismo valor, el dato no hace falta.
     */
    private fun necesitaFase(sistema: SistemaElectrico, magnitudes: List<Magnitud>, salida: Magnitud?): Boolean {
        if (!sistema.esAlterna || salida == null) return false
        val cos = 0.8
        val tension = 230.0
        val corriente = 7.0
        val s = sistema.factor * tension * corriente
        val real = mapOf(
            Magnitud.TENSION to tension,
            Magnitud.CORRIENTE to corriente,
            Magnitud.IMPEDANCIA to tension / corriente,
            Magnitud.RESISTENCIA to tension / corriente * cos,
            Magnitud.POTENCIA_APARENTE to s,
            Magnitud.POTENCIA_ACTIVA to s * cos,
            Magnitud.POTENCIA_REACTIVA to s * sqrt(1 - cos * cos),
        )
        val esperado = real.getValue(salida)
        return runCatching {
            val obtenido = LeyOhm.calcular(LeyOhm.Datos(sistema, magnitudes.associateWith { real.getValue(it) }, 0.5), salida)
            abs(obtenido - esperado) > 1e-9 * esperado
        }.getOrDefault(true)
    }

    private fun ohm(
        id: String,
        salida: Magnitud?,
        sistemas: List<SistemaElectrico>,
        cc: List<Variante>,
        ca: List<Variante>,
        @StringRes intro: Int,
        @StringRes etiqueta: Int,
        simbolo: String,
    ): Calculadora {
        val usados = (cc + ca).flatMap { it.campos }.toSet()
        val campos = todos.values.filter { it.id in usados || (it.id == Ids.FASE && salida != null) }
        return Calculadora(
            id = id,
            sistemas = sistemas,
            campos = campos,
            variantes = { s -> if (s.esAlterna) ca else cc },
            visible = { campo, sistema, variante, unidades ->
                when {
                    variante == null -> false
                    campo.id == Ids.FASE -> necesitaFase(
                        sistema,
                        variante.campos.mapNotNull { magnitud(it, unidades[it]) },
                        salida,
                    )
                    else -> campo.id in variante.campos
                }
            },
            guia = Guia(listOf(intro)),
            calcular = { e ->
                val conocidas = e.variante!!.campos.associate { magnitud(it, e.unidad(it))!! to e.valor(it) }
                val datos = LeyOhm.Datos(e.sistema, conocidas, if (e.tiene(Ids.FASE)) e.valor(Ids.FASE) else 1.0)
                if (salida == null) {
                    val c = LeyOhm.calcularCosPhi(datos)
                    listOf(
                        LineaResultado(etiqueta, Formato.numero(c, 4)),
                        LineaResultado(
                            R.string.res_angulo_desfase,
                            "${Formato.numero(Math.toDegrees(acos(c)), 2)}°",
                            principal = false,
                        ),
                    )
                } else {
                    listOf(LineaResultado(etiqueta, Formato.conPrefijo(LeyOhm.calcular(datos, salida), simbolo)))
                }
            },
        )
    }
}
