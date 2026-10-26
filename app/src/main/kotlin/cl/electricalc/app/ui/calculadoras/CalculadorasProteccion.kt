package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.datos.ric.Ric
import cl.electricalc.datos.ric.VerificacionRic

/** Protección diferencial (RIC N°05, N°10 y N°15) y coordinación con la puesta a tierra en TT (RIC N°05). */
object CalculadorasProteccion {
    private val SIN_SISTEMA = listOf(SistemaElectrico.MONOFASICA)

    private const val APLICACION = "aplicacion"
    private const val RA = "ra"
    private const val SENSIBILIDAD = "sensibilidad"
    private const val LUGAR = "lugar"
    private const val CORRIENTE_TIPO = "corriente_tipo"

    private val aplicacion = CampoOpciones(
        APLICACION, R.string.campo_aplicacion_diferencial, listOf(R.string.ayuda_aplicacion_diferencial),
        opciones = listOf(
            R.string.dif_alumbrado, R.string.dif_no_lineal, R.string.dif_sin_cero, R.string.dif_tt,
            R.string.dif_ve1, R.string.dif_ve2, R.string.dif_ve3, R.string.dif_ve4,
        ).map { TextoOpcion.Recurso(it) },
    )

    /** Tipo, sensibilidad máxima (mA), cita y alternativa para cada opción de [aplicacion]. */
    private data class Regla(val tipo: String, val ma: Int, val cita: String, val alternativa: String? = null)

    private fun regla(opcion: Int): Regla {
        val c = Ric.contactos
        val ve = Ric.diferencialesVe
        fun citaVe(m: Int) = "RIC N°15, numeral ${ve.modo(m).numeral}"
        return when (opcion) {
            0 -> Regla("AC o A", Ric.circuitosAlumbrado.diferencialMaxMa, "RIC N°10, numeral 5.1.3.5")
            1 -> Regla(c.clases.noSenoidal, Ric.circuitosAlumbrado.diferencialMaxMa, "RIC N°05, numeral ${c.clases.numeral}")
            2 -> Regla(c.clases.sinPasoPorCero, Ric.circuitosAlumbrado.diferencialMaxMa, "RIC N°05, numeral ${c.clases.numeral}")
            3 -> Regla("AC o A", c.tt.diferencialMaxMa, "RIC N°05, numeral 8.7.7.4")
            else -> ve.modo(opcion - 3).let { Regla(it.tipo, it.sensibilidadMa, citaVe(it.modo), it.alternativa) }
        }
    }

    val selector = Calculadora(
        id = "selector_diferencial",
        sistemas = SIN_SISTEMA,
        campos = listOf(aplicacion),
        visible = { _, _, _, _ -> true },
        guia = Guia(listOf(R.string.intro_selector_diferencial)),
        calcular = { e ->
            val r = regla(e.opcion(APLICACION))
            listOfNotNull(
                LineaResultado(R.string.res_tipo_diferencial, r.tipo),
                LineaResultado(R.string.res_sensibilidad_max, "≤ ${r.ma} mA"),
                LineaResultado(R.string.res_referencia, r.cita, principal = false),
                r.alternativa?.let { LineaResultado(R.string.res_alternativa, it, principal = false) },
            )
        },
    )

    private val ra = CampoNumerico(
        RA, R.string.campo_ra, listOf(R.string.ayuda_ra),
        unidades = { Unidades.resistencia }, porDefecto = { "Ω" }, hint = { "40" },
    )

    private val sensibilidad = CampoNumerico(
        SENSIBILIDAD, R.string.campo_sensibilidad, listOf(R.string.ayuda_sensibilidad),
        unidades = { Unidades.corriente }, porDefecto = { "mA" }, hint = { "30" }, inicial = { _, _ -> "30" },
    )

    private val lugar = CampoOpciones(
        LUGAR, R.string.campo_lugar, listOf(R.string.ayuda_lugar),
        opciones = listOf(R.string.lugar_seco, R.string.lugar_humedo).map { TextoOpcion.Recurso(it) },
    )

    private val corrienteTipo = CampoOpciones(
        CORRIENTE_TIPO, R.string.campo_tipo_corriente, listOf(R.string.ayuda_tipo_corriente),
        opciones = listOf(R.string.corriente_alterna, R.string.sistema_continua).map { TextoOpcion.Recurso(it) },
    )

    val tt = Calculadora(
        id = "verificacion_tt",
        sistemas = SIN_SISTEMA,
        campos = listOf(ra, sensibilidad, lugar, corrienteTipo),
        visible = { _, _, _, _ -> true },
        guia = Guia(listOf(R.string.intro_verificacion_tt)),
        calcular = { e ->
            val humedo = e.opcion(LUGAR) == 1
            val continua = e.opcion(CORRIENTE_TIPO) == 1
            val ia = e.valor(SENSIBILIDAD)
            val c = Ric.contactos
            listOf(
                LineaResultado(R.string.res_tension_contacto, Formato.conPrefijo(e.valor(RA) * ia, "V")),
                LineaResultado(R.string.res_ra_maxima, Formato.conPrefijo(c.resistenciaMaximaTt(ia, humedo, continua), "Ω")),
                LineaResultado(R.string.res_ul, "${Formato.numero(c.tensionLimite.valor(humedo, continua), 0)} V", principal = false),
            )
        },
        verificar = { e ->
            listOf(VerificacionRic.tt(e.valor(RA), e.valor(SENSIBILIDAD), e.opcion(LUGAR) == 1, e.opcion(CORRIENTE_TIPO) == 1))
        },
    )

    val todas = listOf(selector, tt)
}
