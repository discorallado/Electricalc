package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.conversiones.Awg
import cl.electricalc.calc.conversiones.Conversiones
import cl.electricalc.calc.conversiones.Senoidal
import cl.electricalc.calc.conversiones.Temperatura
import cl.electricalc.calc.conversiones.UnidadLineal
import cl.electricalc.calc.conversiones.Unidades as U
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Conversiones de unidades: lineales (una tabla de factores) y especiales (temperatura, AWG, φ…). */
object CalculadorasConversion {
    /** Seis cifras significativas; notación científica fuera de 10⁻⁴ a 10¹². */
    fun cifras(x: Double): String {
        if (x == 0.0) return "0"
        val exp = floor(log10(abs(x))).toInt()
        if (exp < -4 || exp >= 12) {
            val m = x / Math.pow(10.0, exp.toDouble())
            return "${Formato.numero(m, 5)} × 10^$exp"
        }
        return Formato.numero(x, max(0, 5 - exp))
    }

    private fun Unidad(u: UnidadLineal) = Unidad(u.simbolo) { it * u.factor }

    private fun lineal(id: String, lista: List<UnidadLineal>, inicial: String, intro: Int) = calculadoraSimple(
        id,
        listOf(
            CampoNumerico(
                "v", R.string.campo_valor_convertir, listOf(R.string.ayuda_valor_convertir),
                unidades = { lista.map(::Unidad) }, porDefecto = { lista[inicial.toInt()].simbolo }, hint = { "1" },
            ),
        ),
        listOf(intro),
    ) { e ->
        val base = e.valor("v")
        lista.map { LineaResultado(R.string.res_equivale_a, "${cifras(base / it.factor)} ${it.simbolo}") }
    }

    val potencia = lineal("ConversionePotenza", U.potencia, "1", R.string.intro_conv_potencia)
    val longitud = lineal("ConversioneLunghezza", U.longitud, "0", R.string.intro_conv_longitud)
    val energia = lineal("ConversioneEnergia", U.energia, "3", R.string.intro_conv_energia)
    val presion = lineal("ConversionePressione", U.presion, "3", R.string.intro_conv_presion)
    val magnetico = lineal("ConversioneGaussTesla", U.campoMagnetico, "3", R.string.intro_conv_magnetico)
    val par = lineal("ConversioneMomento", U.par, "0", R.string.intro_conv_par)
    val datos = lineal("ConversioneByte", U.datos, "4", R.string.intro_conv_datos)
    val angulo = lineal("ConversioneAngolo", U.angulo, "0", R.string.intro_conv_angulo)

    val temperatura = calculadoraSimple(
        "ConversioneTemperatura",
        listOf(
            campo("t", R.string.campo_temperatura_conv, R.string.ayuda_temperatura_conv, hint = "25"),
            CampoOpciones("escala", R.string.campo_escala, listOf(R.string.ayuda_escala), listOf("°C", "°F", "K").map { TextoOpcion.Literal(it) }),
        ),
        listOf(R.string.intro_conv_temperatura),
    ) { e ->
        val c = Temperatura.aCelsius(e.valor("t"), e.opcion("escala"))
        listOf(
            LineaResultado(R.string.res_equivale_a, "${Formato.numero(c, 2)} °C"),
            LineaResultado(R.string.res_equivale_a, "${Formato.numero(Temperatura.fahrenheit(c), 2)} °F"),
            LineaResultado(R.string.res_equivale_a, "${Formato.numero(Temperatura.kelvin(c), 2)} K"),
        )
    }

    val tensionPico = calculadoraSimple(
        "ConversioneTensione",
        listOf(
            campo("u", R.string.campo_valor_convertir, R.string.ayuda_valor_senoidal, listOf(Unidades.V), "220"),
            CampoOpciones(
                "tipo", R.string.campo_tipo_valor, listOf(R.string.ayuda_tipo_valor),
                listOf(R.string.valor_eficaz, R.string.valor_pico, R.string.valor_pico_pico, R.string.valor_medio).map { TextoOpcion.Recurso(it) },
            ),
        ),
        listOf(R.string.intro_conv_tension),
    ) { e ->
        val v = e.valor("u")
        val pico = when (e.opcion("tipo")) {
            0 -> Senoidal.picoDeEficaz(v)
            1 -> v
            2 -> v / 2
            else -> v * PI / 2
        }
        listOf(
            LineaResultado(R.string.valor_eficaz, Formato.conPrefijo(Senoidal.eficazDePico(pico), "V")),
            LineaResultado(R.string.valor_pico, Formato.conPrefijo(pico, "V")),
            LineaResultado(R.string.valor_pico_pico, Formato.conPrefijo(2 * pico, "V")),
            LineaResultado(R.string.valor_medio, Formato.conPrefijo(Senoidal.medioDePico(pico), "V")),
        )
    }

    val phi = calculadoraSimple(
        "ConversionePhi",
        listOf(
            campo("v", R.string.campo_valor_convertir, R.string.ayuda_valor_phi, hint = "0,85"),
            CampoOpciones("tipo", R.string.campo_dato_conocido, listOf(R.string.ayuda_dato_phi), listOf("cos φ", "sen φ", "tan φ", "φ (°)").map { TextoOpcion.Literal(it) }),
        ),
        listOf(R.string.intro_conv_phi),
    ) { e ->
        val v = e.valor("v")
        val rad = when (e.opcion("tipo")) {
            0 -> { if (v < 0 || v > 1) throw cl.electricalc.calc.comun.ErrorCalculo(cl.electricalc.calc.comun.MotivoError.VALOR_FUERA_DE_RANGO); acos(v) }
            1 -> { if (v < 0 || v > 1) throw cl.electricalc.calc.comun.ErrorCalculo(cl.electricalc.calc.comun.MotivoError.VALOR_FUERA_DE_RANGO); kotlin.math.asin(v) }
            2 -> kotlin.math.atan(v)
            else -> { if (v < 0 || v > 90) throw cl.electricalc.calc.comun.ErrorCalculo(cl.electricalc.calc.comun.MotivoError.VALOR_FUERA_DE_RANGO); Math.toRadians(v) }
        }
        listOf(
            LineaResultado(R.string.res_phi, "${Formato.numero(Math.toDegrees(rad), 3)}°"),
            LineaResultado(R.string.res_cos_phi, Formato.numero(cos(rad), 4)),
            LineaResultado(R.string.res_sen_phi, Formato.numero(sin(rad), 4)),
            LineaResultado(R.string.res_tan_phi, if (abs(cos(rad)) < 1e-12) "∞" else Formato.numero(tan(rad), 4)),
        )
    }

    val ahKwh = calculadoraSimple(
        "ConversioneAhKwh",
        listOf(
            campo("ah", R.string.campo_capacidad_ah, R.string.ayuda_capacidad_ah, listOf(Unidad("Ah")), "200", opcional = true),
            campo("kwh", R.string.campo_energia_kwh, R.string.ayuda_energia_kwh, listOf(Unidad("kWh")), "2,4", opcional = true),
            campo("vb", R.string.campo_tension_banco, R.string.ayuda_tension_bateria, listOf(Unidades.V), "12"),
        ),
        listOf(R.string.intro_conv_ah),
    ) { e ->
        when {
            e.tiene("ah") -> listOf(LineaResultado(R.string.res_energia, "${Formato.numero(Conversiones.kwh(e.valor("ah"), e.valor("vb")), 3)} kWh"))
            e.tiene("kwh") -> listOf(LineaResultado(R.string.res_capacidad_baterias, "${Formato.numero(Conversiones.ah(e.valor("kwh"), e.valor("vb")), 1)} Ah"))
            else -> throw FaltanDatos()
        }
    }

    val varUf = calculadoraSimple(
        "ConversioneVarUf",
        listOf(
            campo("q", R.string.campo_reactiva_banco, R.string.ayuda_reactiva_conv, listOf(Unidad("kvar") { it * 1e3 }, Unidad("var")), "10", opcional = true),
            campo("c", R.string.campo_capacidad, R.string.ayuda_capacidad_conv, listOf(Unidad("µF") { it / 1e6 }), "73", opcional = true),
            campo("u", R.string.campo_tension_linea, R.string.ayuda_tension_linea, listOf(Unidades.V), "380", inicial = "380"),
            campo("f", R.string.campo_frecuencia_red, R.string.ayuda_frecuencia_red, listOf(Unidad("Hz")), "50", inicial = "50"),
            CampoOpciones("con", R.string.campo_conexion_banco, listOf(R.string.ayuda_conexion_banco), listOf(R.string.conexion_estrella, R.string.conexion_triangulo).map { TextoOpcion.Recurso(it) }, 1),
        ),
        listOf(R.string.intro_conv_var),
    ) { e ->
        val tri = e.opcion("con") == 1
        when {
            e.tiene("q") -> listOf(LineaResultado(R.string.res_capacidad_fase, "${Formato.numero(Conversiones.capacidadBanco(e.valor("q"), e.valor("u"), e.valor("f"), tri) * 1e6, 2)} µF"))
            e.tiene("c") -> listOf(LineaResultado(R.string.res_reactiva_banco, Formato.conPrefijo(Conversiones.reactivaBanco(e.valor("c"), e.valor("u"), e.valor("f"), tri), "var")))
            else -> throw FaltanDatos()
        }
    }

    val rpm = calculadoraSimple(
        "ConversioneRpm",
        listOf(
            campo("n", R.string.campo_velocidad_rotor, R.string.ayuda_rpm_conv, listOf(Unidad("rpm")), "1500"),
            campo("d", R.string.campo_diametro, R.string.ayuda_diametro_polea, listOf(Unidad("mm") { it / 1e3 }, Unidades.m), "200", opcional = true),
        ),
        listOf(R.string.intro_conv_rpm),
    ) { e ->
        val n = e.valor("n")
        listOfNotNull(
            LineaResultado(R.string.res_velocidad_angular, "${Formato.numero(Conversiones.radPorSegundo(n), 3)} rad/s"),
            LineaResultado(R.string.res_frecuencia_giro, "${Formato.numero(n / 60, 3)} Hz"),
            if (e.tiene("d")) LineaResultado(R.string.res_velocidad_tangencial, "${Formato.numero(Conversiones.velocidadTangencial(n, e.valor("d")), 3)} m/s") else null,
        )
    }

    val hzRads = calculadoraSimple(
        "TabConversioneHzRads",
        listOf(
            campo("v", R.string.campo_valor_convertir, R.string.ayuda_hz_rads, hint = "50"),
            CampoOpciones("tipo", R.string.campo_dato_conocido, listOf(R.string.ayuda_hz_rads), listOf("Hz", "rad/s").map { TextoOpcion.Literal(it) }),
        ),
        listOf(R.string.intro_conv_hz),
    ) { e ->
        val f = if (e.opcion("tipo") == 0) e.valor("v") else e.valor("v") / (2 * PI)
        if (!(f > 0)) throw cl.electricalc.calc.comun.ErrorCalculo(cl.electricalc.calc.comun.MotivoError.VALOR_FUERA_DE_RANGO)
        listOf(
            LineaResultado(R.string.res_frecuencia_giro, Formato.conPrefijo(f, "Hz")),
            LineaResultado(R.string.res_velocidad_angular, "${cifras(2 * PI * f)} rad/s"),
            LineaResultado(R.string.res_periodo, Formato.conPrefijo(1 / f, "s"), principal = false),
        )
    }

    val trianguloEstrella = calculadoraSimple(
        "ConversioneTriangoloStella",
        listOf(
            CampoOpciones("sentido", R.string.campo_sentido_conv, listOf(R.string.ayuda_sentido_conv), listOf(R.string.triangulo_a_estrella, R.string.estrella_a_triangulo).map { TextoOpcion.Recurso(it) }),
            campo("a", R.string.campo_r_a, R.string.ayuda_r_delta_y, listOf(Unidades.ohm, Unidades.kOhm), "30"),
            campo("b", R.string.campo_r_b, R.string.ayuda_r_delta_y, listOf(Unidades.ohm, Unidades.kOhm), "30"),
            campo("c", R.string.campo_r_c, R.string.ayuda_r_delta_y, listOf(Unidades.ohm, Unidades.kOhm), "30"),
        ),
        listOf(R.string.intro_conv_delta),
    ) { e ->
        val estrella = e.opcion("sentido") == 0
        val (x, y, z) = if (estrella) Conversiones.trianguloAEstrella(e.valor("a"), e.valor("b"), e.valor("c"))
        else Conversiones.estrellaATriangulo(e.valor("a"), e.valor("b"), e.valor("c"))
        val nombres = if (estrella) listOf("Ra", "Rb", "Rc") else listOf("Rab", "Rbc", "Rca")
        listOf(x, y, z).zip(nombres) { v, n -> LineaResultado(R.string.res_equivale_a, "$n = ${Formato.conPrefijo(v, "Ω")}") }
    }

    val seccion = calculadoraSimple(
        "ConversioneSezione",
        listOf(
            campo("s", R.string.campo_seccion_conv, R.string.ayuda_seccion_conv, listOf(Unidad("mm²"), Unidad("kcmil") { it * Awg.MM2_POR_KCMIL }), "10", opcional = true),
            campo("awg", R.string.campo_awg, R.string.ayuda_awg, hint = "8", opcional = true),
        ),
        listOf(R.string.intro_conv_seccion),
    ) { e ->
        val mm2 = when {
            e.tiene("s") -> e.valor("s")
            e.tiene("awg") -> {
                val n = e.valor("awg")
                if (n != round(n) || n < -3 || n > 40) throw cl.electricalc.calc.comun.ErrorCalculo(cl.electricalc.calc.comun.MotivoError.VALOR_FUERA_DE_RANGO)
                Awg.seccionMm2(n.toInt())
            }
            else -> throw FaltanDatos()
        }
        val calibre = Awg.calibre(mm2)
        val cercano = round(calibre).toInt().coerceIn(-3, 40)
        listOf(
            LineaResultado(R.string.res_seccion_mm2, "${Formato.numero(mm2, 3)} mm²"),
            LineaResultado(R.string.res_kcmil, "${Formato.numero(mm2 / Awg.MM2_POR_KCMIL, 2)} kcmil"),
            LineaResultado(R.string.res_awg_cercano, "${Awg.nombre(cercano)} (${Formato.numero(Awg.seccionMm2(cercano), 2)} mm²)"),
            LineaResultado(R.string.res_diametro_conductor, "${Formato.numero(sqrt(4 * mm2 / PI), 3)} mm", principal = false),
        )
    }

    val todas = listOf(
        potencia, longitud, energia, presion, magnetico, par, datos, angulo,
        temperatura, tensionPico, phi, ahKwh, varUf, rpm, hzRads, trianguloEstrella, seccion,
    )
}
