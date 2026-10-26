package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.protecciones.Coordinacion
import cl.electricalc.calc.protecciones.Cortocircuito
import kotlin.math.sqrt

/**
 * Cortocircuito y coordinación de protecciones por el método de impedancias. El coeficiente k del
 * conductor lo ingresa el usuario desde la norma de su cable; no se usan factores normativos.
 */
object CalculadorasCortocircuito {
    private val c = CalculadoraCaida
    private val kA = listOf(Unidad("kA") { it * 1e3 }, Unidades.A)
    private val tiempos = listOf(Unidad("s"), Unidad("ms") { it / 1e3 })

    private val k = campo("k", R.string.campo_k_cable, R.string.ayuda_k_cable, hint = "")
    private val seccionMm2 = campo("s", R.string.campo_seccion, R.string.ayuda_seccion_cc, listOf(Unidad("mm²")), "10")
    private val icc = campo("icc", R.string.campo_icc, R.string.ayuda_icc, kA, "6")
    private val iccOrigen = campo("icc0", R.string.campo_icc_origen, R.string.ayuda_icc_origen, kA, "6")
    private val iccOrigenOpcional = campo("icc0", R.string.campo_icc_origen, R.string.ayuda_icc_origen_opcional, kA, "6", opcional = true)

    /** Mismos campos de cable que la caída de tensión. */
    private val cable = listOf(c.seccion, c.paralelos, c.longitud, c.material, c.tipoCable, c.temperatura)

    /** R y X de una fase de la línea (Ω), con los paralelos. */
    private fun rx(e: Entrada): Pair<Double, Double> {
        val cab = c.cable(e, e.valor(Ids.LONGITUD))
        val km = e.valor(Ids.LONGITUD) / 1000 / cab.paralelos
        return cab.resistenciaOhmKm(true) * km to cab.reactanciaOhmKm() * km
    }

    val energia = calculadoraSimple("TabEnergiaSpecificaPassante", listOf(k, seccionMm2, campo("i", R.string.campo_icc, R.string.ayuda_icc_tiempo, kA, "6", opcional = true)), listOf(R.string.intro_energia_especifica)) { e ->
        val k2s2 = Cortocircuito.energiaAdmisible(e.valor("k"), e.valor("s"))
        listOfNotNull(
            LineaResultado(R.string.res_k2s2, "${CalculadorasConversion.cifras(k2s2)} A²s"),
            if (e.tiene("i")) LineaResultado(R.string.res_tiempo_maximo, Formato.conPrefijo(Cortocircuito.tiempoMaximo(e.valor("k"), e.valor("s"), e.valor("i")), "s")) else null,
        )
    }

    val proteccionCable = calculadoraSimple(
        "TabProtezioneCavoCortoCircuito",
        listOf(icc, campo("t", R.string.campo_tiempo_despeje, R.string.ayuda_tiempo_despeje, tiempos, "0,1"), k),
        listOf(R.string.intro_proteccion_cable_cc),
    ) { e -> listOf(LineaResultado(R.string.res_seccion_minima_cc, "${Formato.numero(Cortocircuito.seccionMinima(e.valor("icc"), e.valor("t"), e.valor("k")), 2)} mm²")) }

    val dispositivo = calculadoraSimple(
        "TabDimensionamentoDispositivoProtezione",
        listOf(
            campo("ib", R.string.campo_corriente_empleo, R.string.ayuda_ib, listOf(Unidades.A), "14"),
            campo("in", R.string.campo_corriente_nominal_prot, R.string.ayuda_in_prot, listOf(Unidades.A), "16"),
            campo("iz", R.string.campo_corriente_admisible, R.string.ayuda_corriente_admisible, listOf(Unidades.A), "21"),
        ),
        listOf(R.string.intro_dispositivo),
    ) { e ->
        val ok = Coordinacion.cumple(e.valor("ib"), e.valor("in"), e.valor("iz"))
        listOf(
            LineaResultado(R.string.res_coordinacion, if (ok) "Cumple: Ib ≤ In ≤ Iz" else "No cumple: Ib ≤ In ≤ Iz"),
            LineaResultado(R.string.res_margen_conductor, "${Formato.numero((e.valor("iz") - e.valor("in")) / e.valor("iz") * 100, 0)} %", principal = false),
        )
    }

    val transformador = calculadoraSimple(
        "CorrenteCortoCircuitoCabinaTrasformazione",
        listOf(
            campo("s", R.string.campo_potencia_trafo, R.string.ayuda_potencia_trafo_cc, listOf(Unidades.kVA), "630"),
            campo("u", R.string.campo_tension_secundario, R.string.ayuda_tension_secundario, listOf(Unidades.V), "400", inicial = "400"),
            campo("ucc", R.string.campo_impedancia_trafo, R.string.ayuda_impedancia_trafo, listOf(Unidad("%") { it / 100 }), "4", inicial = "4"),
            campo("scc", R.string.campo_scc_red, R.string.ayuda_scc_red, listOf(Unidad("MVA") { it * 1e6 }), "500", opcional = true),
        ),
        listOf(R.string.intro_icc_trafo),
    ) { e ->
        val scc = if (e.tiene("scc")) e.valor("scc") else null
        val i = Cortocircuito.transformador(e.valor("s"), e.valor("u"), e.valor("ucc"), scc)
        listOf(
            LineaResultado(R.string.res_icc_trifasica, Formato.conPrefijo(i, "A")),
            LineaResultado(R.string.res_in_secundario, Formato.conPrefijo(e.valor("s") / (sqrt(3.0) * e.valor("u")), "A"), principal = false),
            LineaResultado(R.string.res_impedancia_trafo, Formato.conPrefijo(Cortocircuito.impedanciaTransformador(e.valor("s"), e.valor("u"), e.valor("ucc")), "Ω"), principal = false),
        )
    }

    private fun linea(id: String, extra: List<Campo>, intro: Int, calcular: (Entrada) -> List<LineaResultado>) = Calculadora(
        id = id, sistemas = listOf(SistemaElectrico.TRIFASICA), campos = extra + cable,
        visible = { _, _, _, _ -> true }, guia = Guia(listOf(intro, R.string.intro_cc_supuestos)), calcular = calcular,
    )

    val aguasAbajo = linea("CorrenteCortoCircuitoConoscendoQuellaAMonte", listOf(Campos.tension, iccOrigen), R.string.intro_icc_aguas_abajo) { e ->
        val (r, x) = rx(e)
        val i = Cortocircuito.aguasAbajo(e.valor("icc0"), e.valor(Ids.TENSION), r, x)
        listOf(
            LineaResultado(R.string.res_icc_trifasica, Formato.conPrefijo(i, "A")),
            LineaResultado(R.string.res_impedancia_linea, "${Formato.numero(r, 4)} + j${Formato.numero(x, 4)} Ω", principal = false),
        )
    }

    private val tensionFn = campo("u0", R.string.campo_tension_fn, R.string.ayuda_tension_fn, listOf(Unidades.V), "220", inicial = "220")

    val minima = linea("CorrenteCortoCircuitoMinima", listOf(tensionFn, iccOrigen), R.string.intro_icc_minima) { e ->
        val (r, x) = rx(e)
        listOf(LineaResultado(R.string.res_icc_minima, Formato.conPrefijo(Cortocircuito.minima(e.valor("icc0"), e.valor("u0"), r, x), "A")))
    }

    val largoMaximo = linea(
        "LunghezzaMassimaCavoCortocircuito",
        listOf(tensionFn, campo("ia", R.string.campo_corriente_disparo, R.string.ayuda_corriente_disparo, listOf(Unidades.A), "160"), iccOrigenOpcional),
        R.string.intro_largo_maximo_cc,
    ) { e ->
        val cab = c.cable(e, 1000.0)
        val rKm = cab.resistenciaOhmKm(true) / cab.paralelos
        val xKm = cab.reactanciaOhmKm() / cab.paralelos
        val l = Cortocircuito.largoMaximo(e.valor("u0"), e.valor("ia"), rKm, xKm, if (e.tiene("icc0")) e.valor("icc0") else null)
        listOf(LineaResultado(R.string.res_largo_maximo, "${Formato.numero(l * 1000, 0)} m"))
    }

    val todas = listOf(energia, proteccionCable, dispositivo, transformador, aguasAbajo, minima, largoMaximo)
}
