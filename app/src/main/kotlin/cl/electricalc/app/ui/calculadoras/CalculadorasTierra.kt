package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.tierra.Resistividad
import cl.electricalc.datos.ric.Ric

/** Puesta a tierra: medición de resistividad (RIC N°06, anexo 6.6) y resistencia de electrodos (tabla N°6.4). */
object CalculadorasTierra {
    /** Estas pantallas no dependen del tipo de corriente: con un solo sistema no se muestra el selector. */
    private val SIN_SISTEMA = listOf(SistemaElectrico.MONOFASICA)

    private const val SEPARACION = "separacion"
    private const val PROFUNDIDAD = "profundidad"
    private const val SEMI_CORRIENTE = "semi_corriente"
    private const val SEMI_POTENCIAL = "semi_potencial"
    private const val RESISTENCIA_MEDIDA = "resistencia_medida"
    private const val ELECTRODO = "electrodo"
    private const val RESISTIVIDAD = "resistividad"
    private const val TERRENO = "terreno"
    private const val MEDIDA = "medida"

    private fun metros(id: String, etiqueta: Int, ayuda: Int, hint: String) = CampoNumerico(
        id, etiqueta, listOf(ayuda),
        unidades = { Unidades.longitud }, porDefecto = { if (it.longitudEnPies) "ft" else "m" }, hint = { hint },
    )

    private val separacion = metros(SEPARACION, R.string.campo_separacion, R.string.ayuda_separacion, "4")
    private val profundidad = metros(PROFUNDIDAD, R.string.campo_profundidad, R.string.ayuda_profundidad, Formato.numero(0.3))
    private val semiCorriente = metros(SEMI_CORRIENTE, R.string.campo_semi_corriente, R.string.ayuda_semi_corriente, "10")
    private val semiPotencial = metros(SEMI_POTENCIAL, R.string.campo_semi_potencial, R.string.ayuda_semi_potencial, "1")

    private val resistenciaMedida = CampoNumerico(
        RESISTENCIA_MEDIDA, R.string.campo_resistencia_medida, listOf(R.string.ayuda_resistencia_medida),
        unidades = { Unidades.resistencia }, porDefecto = { "Ω" }, hint = { "2" },
    )

    private val wennerSimple = Variante(listOf(SEPARACION, RESISTENCIA_MEDIDA), R.string.metodo_wenner)
    private val wennerProfundo = Variante(listOf(SEPARACION, PROFUNDIDAD, RESISTENCIA_MEDIDA), R.string.metodo_wenner_profundo)
    private val schlumberger = Variante(listOf(SEMI_CORRIENTE, SEMI_POTENCIAL, RESISTENCIA_MEDIDA), R.string.metodo_schlumberger)

    val resistividad = Calculadora(
        id = "wenner",
        sistemas = SIN_SISTEMA,
        campos = listOf(separacion, profundidad, semiCorriente, semiPotencial, resistenciaMedida),
        variantes = { listOf(wennerSimple, wennerProfundo, schlumberger) },
        visible = { campo, _, variante, _ -> variante != null && campo.id in variante.campos },
        guia = Guia(listOf(R.string.intro_resistividad, R.string.intro_resistividad_norma)),
        calcular = { e ->
            val r = e.valor(RESISTENCIA_MEDIDA)
            val (rho, ala) = when (e.variante) {
                schlumberger -> Resistividad.schlumberger(e.valor(SEMI_CORRIENTE), e.valor(SEMI_POTENCIAL), r) to e.valor(SEMI_CORRIENTE)
                wennerProfundo -> Resistividad.wennerProfundo(e.valor(SEPARACION), e.valor(PROFUNDIDAD), r) to
                    Resistividad.alaWenner(e.valor(SEPARACION))
                else -> Resistividad.wenner(e.valor(SEPARACION), r) to Resistividad.alaWenner(e.valor(SEPARACION))
            }
            listOf(
                LineaResultado(R.string.res_resistividad, "${Formato.numero(rho, 2)} Ω·m"),
                LineaResultado(R.string.res_ala_medicion, "${Formato.numero(ala, 2)} m", principal = false),
            )
        },
    )

    private val electrodos by lazy { Ric.electrodos.electrodos }
    private val mediosTerreno by lazy { Ric.resistividad.valoresMedios.terrenos }

    private val electrodo = CampoOpciones(
        ELECTRODO, R.string.campo_electrodo, listOf(R.string.ayuda_electrodo),
        opciones = listOf(R.string.electrodo_placa, R.string.electrodo_pica, R.string.electrodo_conductor).map { TextoOpcion.Recurso(it) },
        porDefecto = 1,
    )

    private val resistividadTerreno = CampoNumerico(
        RESISTIVIDAD, R.string.campo_resistividad, listOf(R.string.ayuda_resistividad),
        unidades = { listOf(Unidad("Ω·m")) }, porDefecto = { "Ω·m" }, hint = { "100" },
    )

    private val terreno = CampoOpciones(
        TERRENO, R.string.campo_terreno, listOf(R.string.ayuda_terreno),
        opciones = listOf(
            "Terrenos cultivables y fértiles, terraplenes compactos y húmedos (50 Ω·m)",
            "Terraplenes cultivables poco fértiles y otros terraplenes (500 Ω·m)",
            "Suelos pedregosos desnudos, arenas secas permeables (3.000 Ω·m)",
        ).map { TextoOpcion.Literal(it) },
    )

    private val medida = metros(MEDIDA, R.string.campo_medida_electrodo, R.string.ayuda_medida_electrodo, "2")

    private val conResistividad = Variante(listOf(ELECTRODO, RESISTIVIDAD, MEDIDA), R.string.variante_resistividad_conocida)
    private val conTerreno = Variante(listOf(ELECTRODO, TERRENO, MEDIDA), R.string.variante_valor_medio_terreno)

    val electrodosTierra = Calculadora(
        id = "electrodos_tierra",
        sistemas = SIN_SISTEMA,
        campos = listOf(electrodo, resistividadTerreno, terreno, medida),
        variantes = { listOf(conResistividad, conTerreno) },
        visible = { campo, _, variante, _ -> variante != null && campo.id in variante.campos },
        guia = Guia(listOf(R.string.intro_electrodos, R.string.intro_electrodos_norma)),
        calcular = { e ->
            val tipo = electrodos[e.opcion(ELECTRODO)]
            val rho = if (e.variante == conTerreno) mediosTerreno[e.opcion(TERRENO)].valor else e.valor(RESISTIVIDAD)
            val r = Ric.electrodos.resistencia(tipo.electrodo, rho, e.valor(MEDIDA))
            listOf(
                LineaResultado(R.string.res_resistencia_tierra, "${Formato.numero(r, 2)} Ω"),
                LineaResultado(R.string.res_formula_usada, tipo.formula, principal = false),
                LineaResultado(R.string.res_resistividad_usada, "${Formato.numero(rho, 2)} Ω·m", principal = false),
            )
        },
    )

    val todas = listOf(resistividad, electrodosTierra)
}
