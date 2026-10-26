package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.conductores.Lineas

/** Conductores: resistencia, pérdidas, temperatura de servicio, corriente de empleo y del neutro. */
object CalculadorasConductor {
    private val SISTEMAS = listOf(SistemaElectrico.CONTINUA, SistemaElectrico.MONOFASICA, SistemaElectrico.TRIFASICA)
    private val porciento = Unidad("%") { it / 100 }
    private val c = CalculadoraCaida

    private fun conCable(id: String, extra: List<Campo>, intro: Int, sistemas: List<SistemaElectrico>, calcular: (Entrada) -> List<LineaResultado>) =
        Calculadora(
            id = id, sistemas = sistemas,
            campos = extra + listOf(c.seccion, c.paralelos, c.longitud, c.material, c.tipoCable, c.temperatura),
            visible = { campo, s, _, _ -> campo.id != Ids.FASE || s.esAlterna },
            guia = Guia(listOf(intro)), calcular = calcular,
        )

    /** Resistencia de un conductor de la línea completa (paralelos incluidos), en Ω. */
    private fun resistencia(e: Entrada): Double {
        val cable = c.cable(e, e.valor(Ids.LONGITUD))
        return cable.resistenciaOhmKm(e.sistema.esAlterna) * e.valor(Ids.LONGITUD) / 1000 / cable.paralelos
    }

    val resistenciaConductor = conCable("ResistenzaConduttore", emptyList(), R.string.intro_resistencia_conductor, SISTEMAS) { e ->
        val r = resistencia(e)
        listOf(
            LineaResultado(R.string.res_resistencia_conductor, Formato.conPrefijo(r, "Ω")),
            LineaResultado(R.string.res_resistencia_bucle, Formato.conPrefijo(r * Lineas.conductoresActivos(SistemaElectrico.MONOFASICA), "Ω"), principal = false),
        )
    }

    val perdidas = conCable("PerditePotenzaCavo", listOf(Campos.corriente), R.string.intro_perdidas_cable, SISTEMAS) { e ->
        val p = Lineas.perdidas(e.sistema, e.valor(Ids.CORRIENTE), resistencia(e))
        listOf(
            LineaResultado(R.string.res_perdidas_cable, Formato.conPrefijo(p, "W")),
            LineaResultado(R.string.res_perdidas_anuales, "${Formato.numero(p * 8760 / 1000, 0)} kWh/año", principal = false),
            LineaResultado(R.string.res_resistencia_conductor, Formato.conPrefijo(resistencia(e), "Ω"), principal = false),
        )
    }

    val temperatura = calculadoraSimple(
        "TabTemperaturaCavo",
        listOf(
            campo("ta", R.string.campo_temperatura_ambiente, R.string.ayuda_temperatura_ambiente, listOf(Unidad("°C")), "30", inicial = "30"),
            campo("tm", R.string.campo_temperatura_maxima, R.string.ayuda_temperatura_maxima, listOf(Unidad("°C")), "70", inicial = "70"),
            campo("i", R.string.campo_corriente, R.string.ayuda_corriente_servicio, listOf(Unidades.A), "20"),
            campo("iz", R.string.campo_corriente_admisible, R.string.ayuda_corriente_admisible, listOf(Unidades.A), "32"),
        ),
        listOf(R.string.intro_temperatura_cable),
    ) { e ->
        val t = Lineas.temperatura(e.valor("ta"), e.valor("tm"), e.valor("i"), e.valor("iz"))
        listOf(
            LineaResultado(R.string.res_temperatura_conductor, "${Formato.numero(t, 1)} °C"),
            LineaResultado(R.string.res_carga_relativa, "${Formato.numero(e.valor("i") / e.valor("iz") * 100, 0)} %", principal = false),
        )
    }

    val corrienteEmpleo = Calculadora(
        id = "CorrenteImpiego", sistemas = SISTEMAS,
        campos = listOf(
            Campos.potenciaActiva, Campos.tension, Campos.cosPhi,
            campo("ku", R.string.campo_factor_utilizacion, R.string.ayuda_factor_utilizacion, listOf(porciento), "100", inicial = "100"),
            campo("kc", R.string.campo_factor_simultaneidad, R.string.ayuda_factor_simultaneidad, listOf(porciento), "100", inicial = "100"),
        ),
        visible = { campo, s, _, _ -> campo.id != Ids.FASE || s.esAlterna },
        guia = Guia(listOf(R.string.intro_corriente_empleo)),
        calcular = { e ->
            val ib = Lineas.corrienteEmpleo(
                e.sistema, e.valor(Ids.POTENCIA_ACTIVA), e.valor(Ids.TENSION), c.cosPhi(e), e.valor("ku"), e.valor("kc"),
            )
            listOf(LineaResultado(R.string.res_corriente_empleo, Formato.conPrefijo(ib, "A")))
        },
    )

    val neutro = calculadoraSimple(
        "CorrenteNeutro",
        listOf(
            campo("ia", R.string.campo_corriente_l1, R.string.ayuda_corriente_fase, listOf(Unidades.A), "20"),
            campo("ib", R.string.campo_corriente_l2, R.string.ayuda_corriente_fase, listOf(Unidades.A), "15"),
            campo("ic", R.string.campo_corriente_l3, R.string.ayuda_corriente_fase, listOf(Unidades.A), "10"),
        ),
        listOf(R.string.intro_corriente_neutro),
    ) { e -> listOf(LineaResultado(R.string.res_corriente_neutro, Formato.conPrefijo(Lineas.corrienteNeutro(e.valor("ia"), e.valor("ib"), e.valor("ic")), "A"))) }

    val todas = listOf(resistenciaConductor, perdidas, temperatura, corrienteEmpleo, neutro)
}
