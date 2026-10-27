package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.electronica.CodigoColores

/**
 * Código de colores de resistencias (IEC 60062), con la tabla de la app de referencia: cifras de
 * negro (0) a blanco (9), multiplicador de plata (×0,01) a blanco (×10⁹) y tolerancias por color.
 */
object CalculadorasColores {
    /** Colores en el orden de las cifras 0 a 9. */
    private val CIFRAS = listOf(
        R.string.col_negro, R.string.col_marron, R.string.col_rojo, R.string.col_naranja, R.string.col_amarillo,
        R.string.col_verde, R.string.col_azul, R.string.col_violeta, R.string.col_gris, R.string.col_blanco,
    )

    /** Multiplicador: negro (10⁰) … blanco (10⁹), oro (10⁻¹) y plata (10⁻²). */
    private val MULTIPLICADORES = CIFRAS + listOf(R.string.col_oro, R.string.col_plata)
    private val EXPONENTES = (0..9).toList() + listOf(-1, -2)

    /** Tolerancia en 4 bandas: oro, plata y sin banda. */
    private val TOL_4 = listOf(R.string.col_oro to 5.0, R.string.col_plata to 10.0, R.string.col_sin to 20.0)

    /** Tolerancia en 5 y 6 bandas. */
    private val TOL_5 = listOf(
        R.string.col_marron to 1.0, R.string.col_rojo to 2.0, R.string.col_naranja to 0.05, R.string.col_amarillo to 0.02,
        R.string.col_verde to 0.5, R.string.col_azul to 0.25, R.string.col_violeta to 0.1, R.string.col_gris to 0.01,
        R.string.col_plata to 10.0, R.string.col_oro to 5.0, R.string.col_sin to 20.0,
    )

    private fun opciones(id: String, etiqueta: Int, colores: List<Int>, porDefecto: Int = 0) =
        CampoOpciones(id, etiqueta, listOf(R.string.ayuda_banda), colores.map { TextoOpcion.Recurso(it) }, porDefecto)

    private val cuatro = Variante(listOf("c1", "c2", "mult", "tol4"), R.string.bandas_4)
    private val cinco = Variante(listOf("c1", "c2", "c3", "mult", "tol5"), R.string.bandas_5)

    val lectura = Calculadora(
        id = "CodiceColoreResistori",
        sistemas = listOf(SistemaElectrico.MONOFASICA),
        campos = listOf(
            opciones("c1", R.string.campo_banda_1, CIFRAS, 4),
            opciones("c2", R.string.campo_banda_2, CIFRAS, 7),
            opciones("c3", R.string.campo_banda_3, CIFRAS),
            opciones("mult", R.string.campo_multiplicador, MULTIPLICADORES, 2),
            opciones("tol4", R.string.campo_tolerancia, TOL_4.map { it.first }),
            opciones("tol5", R.string.campo_tolerancia, TOL_5.map { it.first }),
        ),
        variantes = { listOf(cuatro, cinco) },
        visible = { campo, _, variante, _ -> variante != null && campo.id in variante.campos },
        guia = Guia(listOf(R.string.intro_colores_lectura)),
        calcular = { e ->
            val ids = if (e.variante === cinco) listOf("c1", "c2", "c3") else listOf("c1", "c2")
            val ohm = CodigoColores.valor(ids.map { e.opcion(it) }, EXPONENTES[e.opcion("mult")])
            val tol = if (e.variante === cinco) TOL_5[e.opcion("tol5")].second else TOL_4[e.opcion("tol4")].second
            listOf(
                LineaResultado(R.string.res_resistencia_colores, Formato.conPrefijo(ohm, "Ω")),
                LineaResultado(R.string.res_tolerancia, "± ${Formato.numero(tol, 2)} %"),
                LineaResultado(
                    R.string.res_rango, "${Formato.conPrefijo(ohm * (1 - tol / 100), "Ω")} … ${Formato.conPrefijo(ohm * (1 + tol / 100), "Ω")}",
                    principal = false,
                ),
            )
        },
    )

    private fun nombres(cifras: List<Int>, exponente: Int) =
        (cifras.map { CIFRAS[it] } + MULTIPLICADORES[EXPONENTES.indexOf(exponente)]).map { TextoRecurso(it) }

    val desdeValor = calculadoraSimple(
        "ColoreResistoreDaValore",
        listOf(campo("r", R.string.campo_resistencia, R.string.ayuda_valor_resistencia, listOf(Unidades.ohm, Unidades.kOhm, Unidad("MΩ") { it * 1e6 }), "4,7")),
        listOf(R.string.intro_colores_valor),
    ) { e ->
        val r = e.valor("r")
        val (c4, x4) = CodigoColores.bandas(r, 2)
        val (c5, x5) = CodigoColores.bandas(r, 3)
        listOf(
            LineaResultado.texto(R.string.res_bandas_4, R.string.val_colores_3, *nombres(c4, x4).toTypedArray<Any>()),
            LineaResultado.texto(R.string.res_bandas_5, R.string.val_colores_4, *nombres(c5, x5).toTypedArray<Any>()),
        )
    }

    val todas = listOf(lectura, desdeValor)
}

/** Código de resistencias SMD (3 y 4 cifras, R/K/M, EIA-96). */
object CalculadoraSmd {
    private val tipos = mapOf(
        cl.electricalc.calc.electronica.CodigoSmd.Tipo.PUENTE to R.string.smd_puente,
        cl.electricalc.calc.electronica.CodigoSmd.Tipo.CIFRAS to R.string.smd_cifras,
        cl.electricalc.calc.electronica.CodigoSmd.Tipo.EIA96 to R.string.smd_eia96,
        cl.electricalc.calc.electronica.CodigoSmd.Tipo.LETRA_DELANTE to R.string.smd_letra,
    )

    val smd = calculadoraSimple(
        "CodiceResistoriSMD",
        listOf(
            CampoTexto("codigo", R.string.campo_codigo_smd, listOf(R.string.ayuda_codigo_smd), "4K7"),
            CampoOpciones("letra", R.string.campo_letra_tolerancia, listOf(R.string.ayuda_letra_tolerancia), listOf(R.string.no, R.string.si).map { TextoOpcion.Recurso(it) }),
        ),
        listOf(R.string.intro_smd),
    ) { e ->
        val l = cl.electricalc.calc.electronica.CodigoSmd.leer(e.texto("codigo"), e.opcion("letra") == 1)
        listOfNotNull(
            LineaResultado(R.string.res_resistencia_colores, Formato.conPrefijo(l.ohm, "Ω")),
            l.toleranciaPct?.let { LineaResultado(R.string.res_tolerancia, "± ${Formato.numero(it, 2)} %") },
            LineaResultado.texto(R.string.res_tipo_codigo, tipos.getValue(l.tipo), principal = false),
        )
    }
}

/** Código impreso en condensadores (pF, R/P/N/U, tolerancia y tensión). */
object CalculadoraCondensador {
    val codigo = calculadoraSimple(
        "CodiceCondensatori",
        listOf(CampoTexto("codigo", R.string.campo_codigo_condensador, listOf(R.string.ayuda_codigo_condensador), "104J")),
        listOf(R.string.intro_codigo_condensador),
    ) { e ->
        val l = cl.electricalc.calc.electronica.CodigoCondensador.leer(e.texto("codigo"))
        listOfNotNull(
            LineaResultado(R.string.res_capacidad, Formato.conPrefijo(l.pf * 1e-12, "F")),
            LineaResultado(R.string.res_capacidad_pf, "${Formato.numero(l.pf, 2)} pF", principal = false),
            l.tolerancia?.let { LineaResultado(R.string.res_tolerancia, it) },
            l.tensionV?.let { LineaResultado(R.string.res_tension_nominal, "${Formato.numero(it, 1)} V") },
        )
    }
}
