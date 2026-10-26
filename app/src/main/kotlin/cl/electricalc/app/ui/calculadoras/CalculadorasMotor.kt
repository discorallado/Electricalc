package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.motores.Motor
import kotlin.math.acos

/** Motores: corriente, potencia, tensión, cos φ, rendimiento, velocidad, deslizamiento y par. */
object CalculadorasMotor {
    private val SISTEMAS = listOf(SistemaElectrico.CONTINUA, SistemaElectrico.MONOFASICA, SistemaElectrico.TRIFASICA)
    private val porciento = Unidad("%") { it / 100 }
    private const val W_POR_HP = 745.7

    private val potencia = CampoNumerico(
        "pm", R.string.campo_potencia_motor, listOf(R.string.ayuda_potencia_motor),
        unidades = { Unidades.potenciaActiva }, porDefecto = { "kW" }, hint = { "7,5" },
    )
    private val cos = campo("cos", R.string.campo_factor_potencia, R.string.ayuda_cos_motor, hint = "0,85", inicial = Formato.numero(0.85))
    private val eta = campo("eta", R.string.campo_rendimiento_motor, R.string.ayuda_rendimiento_motor, listOf(porciento), "90", inicial = "90")
    private val frecuencia = campo("f", R.string.campo_frecuencia_red, R.string.ayuda_frecuencia_red, listOf(Unidad("Hz")), "50", inicial = "50")
    private val polos = campo("polos", R.string.campo_polos, R.string.ayuda_polos, hint = "4", inicial = "4")
    private val rpm = campo("n", R.string.campo_velocidad_rotor, R.string.ayuda_velocidad_rotor, listOf(Unidad("rpm")), "1440")

    private fun motor(id: String, campos: List<Campo>, intro: Int, calcular: (Entrada) -> List<LineaResultado>) = Calculadora(
        id = id, sistemas = SISTEMAS, campos = campos,
        visible = { c, s, _, _ -> c.id != "cos" || s.esAlterna },
        guia = Guia(listOf(intro, R.string.intro_motor_comun)), calcular = calcular,
    )

    private fun Entrada.cos() = if (tiene("cos")) valor("cos") else 1.0

    private fun Entrada.polos(): Int {
        val p = valor("polos")
        if (p != Math.rint(p)) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return p.toInt()
    }

    private fun hp(w: Double) = "${Formato.numero(w / W_POR_HP, 2)} HP"

    val corriente = motor("CorrenteMotore", listOf(potencia, Campos.tension, cos, eta), R.string.intro_corriente_motor) { e ->
        val i = Motor.corriente(e.sistema, e.valor("pm"), e.valor(Ids.TENSION), e.cos(), e.valor("eta"))
        listOf(
            LineaResultado(R.string.res_corriente_motor, Formato.conPrefijo(i, "A")),
            LineaResultado(R.string.res_potencia_absorbida, Formato.conPrefijo(e.valor("pm") / e.valor("eta"), "W"), principal = false),
        )
    }

    val potenciaMotor = motor("PotenzaMotore", listOf(Campos.tension, Campos.corriente, cos, eta), R.string.intro_potencia_motor) { e ->
        val p = Motor.potencia(e.sistema, e.valor(Ids.TENSION), e.valor(Ids.CORRIENTE), e.cos(), e.valor("eta"))
        listOf(
            LineaResultado(R.string.res_potencia_eje, Formato.conPrefijo(p, "W")),
            LineaResultado(R.string.res_potencia_hp, hp(p), principal = false),
            LineaResultado(R.string.res_potencia_absorbida, Formato.conPrefijo(p / e.valor("eta"), "W"), principal = false),
        )
    }

    val tension = motor("TensioneMotore", listOf(potencia, Campos.corriente, cos, eta), R.string.intro_tension_motor) { e ->
        val u = Motor.tension(e.sistema, e.valor("pm"), e.valor(Ids.CORRIENTE), e.cos(), e.valor("eta"))
        listOf(LineaResultado(R.string.res_tension_motor, Formato.conPrefijo(u, "V")))
    }

    val factorPotencia = Calculadora(
        id = "FattorePotenzaMotore", sistemas = SISTEMAS.drop(1), campos = listOf(potencia, Campos.tension, Campos.corriente, eta),
        visible = { _, _, _, _ -> true }, guia = Guia(listOf(R.string.intro_fp_motor, R.string.intro_motor_comun)),
        calcular = { e ->
            val c = Motor.factorPotencia(e.sistema, e.valor("pm"), e.valor(Ids.TENSION), e.valor(Ids.CORRIENTE), e.valor("eta"))
            listOf(
                LineaResultado(R.string.res_cos_motor, Formato.numero(c, 3)),
                LineaResultado(R.string.res_angulo_motor, "${Formato.numero(Math.toDegrees(acos(c)), 1)}°", principal = false),
            )
        },
    )

    val rendimiento = motor("RendimentoMotore", listOf(potencia, Campos.tension, Campos.corriente, cos), R.string.intro_rendimiento_motor) { e ->
        val r = Motor.rendimiento(e.sistema, e.valor("pm"), e.valor(Ids.TENSION), e.valor(Ids.CORRIENTE), e.cos())
        listOf(
            LineaResultado(R.string.res_rendimiento_motor, "${Formato.numero(r * 100, 1)} %"),
            LineaResultado(R.string.res_perdidas_motor, Formato.conPrefijo(e.valor("pm") / r - e.valor("pm"), "W"), principal = false),
        )
    }

    val velocidad = calculadoraSimple(
        "VelocitaMotore",
        listOf(frecuencia, polos, campo("s", R.string.campo_deslizamiento, R.string.ayuda_deslizamiento, listOf(porciento), "4", inicial = "0")),
        listOf(R.string.intro_velocidad_motor),
    ) { e ->
        val ns = Motor.velocidadSincrona(e.valor("f"), e.polos())
        listOf(
            LineaResultado(R.string.res_velocidad_rotor, "${Formato.numero(Motor.velocidad(e.valor("f"), e.polos(), e.valor("s")), 0)} rpm"),
            LineaResultado(R.string.res_velocidad_sincrona, "${Formato.numero(ns, 0)} rpm", principal = false),
        )
    }

    val deslizamiento = calculadoraSimple("ScorrimentoMotore", listOf(frecuencia, polos, rpm), listOf(R.string.intro_deslizamiento)) { e ->
        val s = Motor.deslizamiento(e.valor("f"), e.polos(), e.valor("n"))
        listOf(
            LineaResultado(R.string.res_deslizamiento, "${Formato.numero(s * 100, 2)} %"),
            LineaResultado(R.string.res_velocidad_sincrona, "${Formato.numero(Motor.velocidadSincrona(e.valor("f"), e.polos()), 0)} rpm", principal = false),
        )
    }

    val par = calculadoraSimple(
        "CoppiaMassima",
        listOf(
            CampoNumerico(
                "pm", R.string.campo_potencia_motor, listOf(R.string.ayuda_potencia_par),
                unidades = { Unidades.potenciaActiva }, porDefecto = { "kW" }, hint = { "7,5" }, opcional = true,
            ),
            campo("t", R.string.campo_par, R.string.ayuda_par, listOf(Unidad("N·m")), "50", opcional = true),
            rpm,
        ),
        listOf(R.string.intro_par),
    ) { e ->
        when {
            e.tiene("pm") -> listOf(LineaResultado(R.string.res_par, "${Formato.numero(Motor.par(e.valor("pm"), e.valor("n")), 2)} N·m"))
            e.tiene("t") -> {
                val p = Motor.potenciaDePar(e.valor("t"), e.valor("n"))
                listOf(LineaResultado(R.string.res_potencia_eje, Formato.conPrefijo(p, "W")), LineaResultado(R.string.res_potencia_hp, hp(p), principal = false))
            }
            else -> throw FaltanDatos()
        }
    }

    val transformador = calculadoraSimple(
        "PrimarioSecondarioTrasformatore",
        listOf(
            campo("u1", R.string.campo_tension_primario, R.string.ayuda_u1_relacion, listOf(Unidades.V, Unidades.kV), "220"),
            campo("u2", R.string.campo_tension_secundario, R.string.ayuda_u2_relacion, listOf(Unidades.V, Unidades.kV), "24"),
            campo("n1", R.string.campo_espiras_primario, R.string.ayuda_espiras, hint = "500", opcional = true),
            campo("i2", R.string.campo_corriente_secundario, R.string.ayuda_i2_relacion, listOf(Unidades.A), "5", opcional = true),
        ),
        listOf(R.string.intro_relacion_trafo),
    ) { e ->
        val a = e.valor("u1") / e.valor("u2")
        if (!(a > 0) || a.isInfinite()) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        listOfNotNull(
            LineaResultado(R.string.res_relacion_transformacion, Formato.numero(a, 3)),
            if (e.tiene("n1")) LineaResultado(R.string.res_espiras_secundario, Formato.numero(e.valor("n1") / a, 0)) else null,
            if (e.tiene("i2")) LineaResultado(R.string.res_corriente_primario, Formato.conPrefijo(e.valor("i2") / a, "A")) else null,
        )
    }

    val todas = listOf(corriente, potenciaMotor, tension, factorPotencia, rendimiento, velocidad, deslizamiento, par, transformador)
}
