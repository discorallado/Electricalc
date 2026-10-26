package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.electronica.Asociacion
import cl.electricalc.calc.electronica.Circuitos
import cl.electricalc.calc.electronica.Reactancia

/** Fundamentos y electrónica: reactancias, asociaciones, divisores, LED, zener, Joule y antenas. */
object CalculadorasElectronica {
    private val capacidad = listOf(Unidad("µF") { it / 1e6 }, Unidad("nF") { it / 1e9 }, Unidad("pF") { it / 1e12 }, Unidad("mF") { it / 1e3 }, Unidad("F"))
    private val inductancia = listOf(Unidad("mH") { it / 1e3 }, Unidad("µH") { it / 1e6 }, Unidad("H"))
    private val frecuencias = listOf(Unidad("Hz"), Unidad("kHz") { it * 1e3 }, Unidad("MHz") { it * 1e6 })
    private val ohmios = listOf(Unidades.ohm, Unidades.kOhm, Unidades.mOhm)
    private val corrientes = listOf(Unidad("mA") { it / 1e3 }, Unidades.A)
    private val voltios = listOf(Unidades.V)

    private fun Entrada.opcionales(prefijo: String) = (1..6).map { "$prefijo$it" }.filter { tiene(it) }.map { valor(it) }

    val reactancia = calculadoraSimple(
        "Reattanza",
        listOf(
            campo("f", R.string.campo_frecuencia_red, R.string.ayuda_frecuencia_x, frecuencias, "50", inicial = "50"),
            campo("l", R.string.campo_inductancia, R.string.ayuda_inductancia_x, inductancia, "100", opcional = true),
            campo("c", R.string.campo_capacidad, R.string.ayuda_capacidad_x, capacidad, "100", opcional = true),
        ),
        listOf(R.string.intro_reactancia),
    ) { e ->
        if (!e.tiene("l") && !e.tiene("c")) throw FaltanDatos()
        val xl = if (e.tiene("l")) Reactancia.inductiva(e.valor("f"), e.valor("l")) else null
        val xc = if (e.tiene("c")) Reactancia.capacitiva(e.valor("f"), e.valor("c")) else null
        listOfNotNull(
            xl?.let { LineaResultado(R.string.res_reactancia_inductiva, Formato.conPrefijo(it, "Ω")) },
            xc?.let { LineaResultado(R.string.res_reactancia_capacitiva, Formato.conPrefijo(it, "Ω")) },
            if (xl != null && xc != null) LineaResultado(R.string.res_reactancia_neta, "${Formato.numero(xl - xc, 3)} Ω", principal = false) else null,
        )
    }

    val impedancia = calculadoraSimple(
        "ImpedenzaDaResistenzaEReattanza",
        listOf(
            campo("r", R.string.campo_resistencia, R.string.ayuda_r_impedancia, ohmios, "3"),
            campo("x", R.string.campo_reactancia, R.string.ayuda_x_impedancia, ohmios, "4"),
        ),
        listOf(R.string.intro_impedancia_rx),
    ) { e ->
        val z = Reactancia.impedancia(e.valor("r"), e.valor("x"))
        val fi = Reactancia.angulo(e.valor("r"), e.valor("x"))
        listOf(
            LineaResultado(R.string.res_impedancia, Formato.conPrefijo(z, "Ω")),
            LineaResultado(R.string.res_angulo_impedancia, "${Formato.numero(fi, 2)}°", principal = false),
            LineaResultado(R.string.res_cos_impedancia, Formato.numero(e.valor("r") / z, 3), principal = false),
        )
    }

    val joule = calculadoraSimple(
        "EffettoJoule",
        listOf(
            campo("i", R.string.campo_corriente, R.string.ayuda_i_joule, corrientes.reversed(), "10"),
            campo("r", R.string.campo_resistencia, R.string.ayuda_r_joule, ohmios, "2"),
            campo("t", R.string.campo_tiempo, R.string.ayuda_tiempo_joule, listOf(Unidad("s"), Unidad("min") { it * 60 }, Unidad("h") { it * 3600 }), "60"),
        ),
        listOf(R.string.intro_joule),
    ) { e ->
        val q = Circuitos.joule(e.valor("i"), e.valor("r"), e.valor("t"))
        listOf(
            LineaResultado(R.string.res_energia_joule, Formato.conPrefijo(q, "J")),
            LineaResultado(R.string.res_potencia_disipada, Formato.conPrefijo(e.valor("i") * e.valor("i") * e.valor("r"), "W"), principal = false),
            LineaResultado(R.string.res_energia_wh, "${Formato.numero(q / 3600, 3)} Wh", principal = false),
            LineaResultado(R.string.res_calorias, "${Formato.numero(q / 4.184, 1)} cal", principal = false),
        )
    }

    private fun suma(id: String, prefijo: String, unidades: List<Unidad>, etiqueta: Int, intro: Int, serieSuma: Boolean, simbolo: String) = calculadoraSimple(
        id,
        listOf(
            CampoOpciones("conexion", R.string.campo_conexion, listOf(R.string.ayuda_conexion), listOf(R.string.conexion_serie, R.string.conexion_paralelo).map { TextoOpcion.Recurso(it) }),
        ) + (1..6).map { campo("$prefijo$it", etiqueta, R.string.ayuda_valor_componente, unidades, if (it <= 2) "10" else "", opcional = it > 2) },
        listOf(intro),
    ) { e ->
        val valores = e.opcionales(prefijo)
        val serie = e.opcion("conexion") == 0
        val total = if (serie == serieSuma) Asociacion.suma(valores) else Asociacion.inversa(valores)
        listOf(LineaResultado(R.string.res_equivalente, Formato.conPrefijo(total, simbolo)))
    }

    val sumaResistencias = suma("TabSommaResistenze", "r", ohmios, R.string.campo_resistencia, R.string.intro_suma_resistencias, serieSuma = true, simbolo = "Ω")
    val sumaCondensadores = suma("TabSommaCondensatori", "c", capacidad, R.string.campo_capacidad, R.string.intro_suma_condensadores, serieSuma = false, simbolo = "F")

    val resonancia = calculadoraSimple(
        "FrequenzaRisonanza",
        listOf(
            campo("l", R.string.campo_inductancia, R.string.ayuda_inductancia_x, inductancia, "1"),
            campo("c", R.string.campo_capacidad, R.string.ayuda_capacidad_x, capacidad, "1"),
        ),
        listOf(R.string.intro_resonancia),
    ) { e -> listOf(LineaResultado(R.string.res_frecuencia_resonancia, Formato.conPrefijo(Reactancia.resonancia(e.valor("l"), e.valor("c")), "Hz"))) }

    val condensador = calculadoraSimple(
        "PotenzaResaCondensatore",
        listOf(
            campo("u", R.string.campo_tension, R.string.ayuda_u_condensador, listOf(Unidades.V, Unidades.kV), "380"),
            campo("f", R.string.campo_frecuencia_red, R.string.ayuda_frecuencia_red, frecuencias, "50", inicial = "50"),
            campo("c", R.string.campo_capacidad, R.string.ayuda_capacidad_x, capacidad, "50"),
        ),
        listOf(R.string.intro_potencia_condensador),
    ) { e -> listOf(LineaResultado(R.string.res_reactiva_condensador, Formato.conPrefijo(Reactancia.potenciaCondensador(e.valor("u"), e.valor("f"), e.valor("c")), "var"))) }

    val divisorTension = calculadoraSimple(
        "PartitoreTensione",
        listOf(
            campo("vin", R.string.campo_tension_entrada, R.string.ayuda_vin, voltios, "12"),
            campo("r1", R.string.campo_r1, R.string.ayuda_r1_divisor, ohmios, "10"),
            campo("r2", R.string.campo_r2, R.string.ayuda_r2_divisor, ohmios, "10"),
        ),
        listOf(R.string.intro_divisor_tension),
    ) { e ->
        val vout = Circuitos.divisorTension(e.valor("vin"), e.valor("r1"), e.valor("r2"))
        listOf(
            LineaResultado(R.string.res_tension_salida, Formato.conPrefijo(vout, "V")),
            LineaResultado(R.string.res_corriente_divisor, Formato.conPrefijo(e.valor("vin") / (e.valor("r1") + e.valor("r2")), "A"), principal = false),
        )
    }

    val divisorCorriente = calculadoraSimple(
        "PartitoreCorrente",
        listOf(
            campo("i", R.string.campo_corriente_total, R.string.ayuda_i_divisor, corrientes.reversed(), "1"),
            campo("r1", R.string.campo_r1, R.string.ayuda_r_paralelo, ohmios, "30"),
            campo("r2", R.string.campo_r2, R.string.ayuda_r_paralelo, ohmios, "20"),
        ),
        listOf(R.string.intro_divisor_corriente),
    ) { e ->
        val i1 = Circuitos.divisorCorriente(e.valor("i"), e.valor("r1"), e.valor("r2"))
        listOf(
            LineaResultado(R.string.res_corriente_r1, Formato.conPrefijo(i1, "A")),
            LineaResultado(R.string.res_corriente_r2, Formato.conPrefijo(e.valor("i") - i1, "A")),
        )
    }

    private fun resistenciaResultado(r: Double, i: Double) = listOf(
        LineaResultado(R.string.res_resistencia_serie, Formato.conPrefijo(r, "Ω")),
        LineaResultado(R.string.res_potencia_resistencia, Formato.conPrefijo(r * i * i, "W"), principal = false),
    )

    val zener = calculadoraSimple(
        "StabilizzatoreTensioneDiodoZener",
        listOf(
            campo("vin", R.string.campo_tension_entrada, R.string.ayuda_vin, voltios, "12"),
            campo("vz", R.string.campo_tension_zener, R.string.ayuda_vz, voltios, "5,1"),
            campo("iz", R.string.campo_corriente_zener, R.string.ayuda_iz, corrientes, "5"),
            campo("il", R.string.campo_corriente_carga, R.string.ayuda_il, corrientes, "20"),
        ),
        listOf(R.string.intro_zener),
    ) { e ->
        val i = e.valor("iz") + e.valor("il")
        resistenciaResultado(Circuitos.resistenciaZener(e.valor("vin"), e.valor("vz"), e.valor("iz"), e.valor("il")), i) +
            LineaResultado(R.string.res_potencia_zener, Formato.conPrefijo(e.valor("vz") * i, "W"), principal = false)
    }

    val reducirTension = calculadoraSimple(
        "ResistenzaRidurreTensione",
        listOf(
            campo("vin", R.string.campo_tension_entrada, R.string.ayuda_vin, voltios, "12"),
            campo("vout", R.string.campo_tension_salida, R.string.ayuda_vout, voltios, "9"),
            campo("i", R.string.campo_corriente_carga, R.string.ayuda_il, corrientes, "100"),
        ),
        listOf(R.string.intro_reducir_tension),
    ) { e -> resistenciaResultado(Circuitos.resistenciaSerie(e.valor("vin"), e.valor("vout"), e.valor("i")), e.valor("i")) }

    val led = calculadoraSimple(
        "ResistenzaLed",
        listOf(
            campo("vs", R.string.campo_tension_fuente, R.string.ayuda_vs_led, voltios, "5"),
            campo("vf", R.string.campo_tension_led, R.string.ayuda_vf_led, voltios, "2"),
            campo("i", R.string.campo_corriente_led, R.string.ayuda_i_led, corrientes, "20"),
            campo("n", R.string.campo_numero_led, R.string.ayuda_numero_led, hint = "1", inicial = "1"),
        ),
        listOf(R.string.intro_led),
    ) { e ->
        val n = e.valor("n")
        if (n != Math.rint(n)) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        resistenciaResultado(Circuitos.resistenciaLed(e.valor("vs"), e.valor("vf"), e.valor("i"), n.toInt()), e.valor("i"))
    }

    val antena = calculadoraSimple(
        "LunghezzaAntenna",
        listOf(
            campo("f", R.string.campo_frecuencia_red, R.string.ayuda_frecuencia_antena, frecuencias.reversed(), "100"),
            campo("vf", R.string.campo_factor_velocidad, R.string.ayuda_factor_velocidad, hint = "0,95", inicial = Formato.numero(0.95)),
        ),
        listOf(R.string.intro_antena),
    ) { e ->
        val l = Circuitos.longitudOnda(e.valor("f"), e.valor("vf"))
        listOf(
            LineaResultado(R.string.res_media_onda, "${Formato.numero(l / 2, 3)} m"),
            LineaResultado(R.string.res_cuarto_onda, "${Formato.numero(l / 4, 3)} m"),
            LineaResultado(R.string.res_onda_completa, "${Formato.numero(l, 3)} m", principal = false),
        )
    }

    val senal = calculadoraSimple(
        "ValoriSegnaliAnalogici",
        listOf(
            campo("x", R.string.campo_senal, R.string.ayuda_senal, hint = "12"),
            campo("xmin", R.string.campo_senal_min, R.string.ayuda_rango_senal, hint = "4", inicial = "4"),
            campo("xmax", R.string.campo_senal_max, R.string.ayuda_rango_senal, hint = "20", inicial = "20"),
            campo("ymin", R.string.campo_escala_min, R.string.ayuda_rango_escala, hint = "0", inicial = "0"),
            campo("ymax", R.string.campo_escala_max, R.string.ayuda_rango_escala, hint = "100", inicial = "100"),
        ),
        listOf(R.string.intro_senal),
    ) { e ->
        val y = Circuitos.escalar(e.valor("x"), e.valor("xmin"), e.valor("xmax"), e.valor("ymin"), e.valor("ymax"))
        val pct = (e.valor("x") - e.valor("xmin")) / (e.valor("xmax") - e.valor("xmin")) * 100
        listOf(
            LineaResultado(R.string.res_valor_escalado, Formato.numero(y, 3)),
            LineaResultado(R.string.res_porcentaje_rango, "${Formato.numero(pct, 1)} %", principal = false),
        )
    }

    val todas = listOf(
        reactancia, impedancia, joule, sumaResistencias, sumaCondensadores, resonancia, condensador,
        divisorTension, divisorCorriente, zener, reducirTension, led, antena, senal,
    )
}
