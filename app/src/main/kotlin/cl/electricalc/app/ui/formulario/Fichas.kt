package cl.electricalc.app.ui.formulario

import cl.electricalc.app.R
import java.util.Locale

/**
 * Fichas del formulario, por id del catálogo. Las de ley de Ohm comparten la convención del motor:
 * U es la tensión entre fases (bifásica y trifásica, con factor √3 en trifásica) y U₀ la tensión
 * fase-neutro en monofásica.
 */
object Fichas {
    private val sen get() = if (Locale.getDefault().language == "es") "sen" else "sin"

    /** Antepone √3 × en trifásica. */
    private fun k(tri: Boolean, resto: String): Expr = if (tri) e(raiz("3"), " × $resto") else e(resto)

    private fun ohm(cc: ((String) -> List<Expr>)?, ca: (u: String, tri: Boolean) -> List<Expr>): Ficha {
        val tarjetas = buildList {
            if (cc != null) add(TarjetaFicha(R.string.sistema_continua, cc("U")))
            add(TarjetaFicha(R.string.sistema_monofasica, ca("U_0", false)))
            add(TarjetaFicha(R.string.sistema_bifasica, ca("U", false)))
            add(TarjetaFicha(R.string.sistema_trifasica, ca("U", true)))
        }
        return Ficha(tarjetas, leyendaOhm)
    }

    private val leyendaOhm = listOf(
        Simbolo("I", R.string.sim_corriente, "A"),
        Simbolo("U_0", R.string.sim_tension_fn, "V"),
        Simbolo("U", R.string.sim_tension_ff, "V"),
        Simbolo("P", R.string.sim_potencia_activa, "W"),
        Simbolo("S", R.string.sim_potencia_aparente, "VA"),
        Simbolo("Q", R.string.sim_potencia_reactiva, "var"),
        Simbolo("R", R.string.sim_resistencia, "Ω"),
        Simbolo("Z", R.string.sim_impedancia, "Ω"),
        Simbolo("cos φ", R.string.sim_factor_potencia),
        Simbolo("φ", R.string.sim_desfase, "°"),
    )

    private fun corriente() = ohm(
        cc = { u -> listOf(e("I = ", frac("P", u)), e("I = ", raiz(frac("P", "R"))), e("I = ", frac(u, "R"))) },
    ) { u, t ->
        listOf(
            e("I = ", frac("P", k(t, "$u × cos φ"))),
            e("I = ", raiz(frac("P", k(t, "Z × cos φ")))),
            e("I = ", raiz(frac("P", k(t, "R")))),
            e("I = ", frac(u, "Z")),
            e("I = ", frac("$u × cos φ", "R")),
            e("I = ", frac("S", k(t, u))),
            e("I = ", raiz(frac("S", k(t, "Z")))),
            e("I = ", raiz(frac("S × cos φ", k(t, "R")))),
            e("I = ", frac("Q", k(t, "$u × $sen φ"))),
            e("I = ", raiz(frac("Q", k(t, "Z × $sen φ")))),
            e("I = ", raiz(frac("Q × cos φ", k(t, "R × $sen φ")))),
        )
    }

    private fun tension() = ohm(
        cc = { u -> listOf(e("$u = ", frac("P", "I")), e("$u = R × I"), e("$u = ", raiz("P × R"))) },
    ) { u, t ->
        listOf(
            e("$u = ", frac("P", k(t, "I × cos φ"))),
            e("$u = Z × I"),
            e("$u = ", frac("R × I", "cos φ")),
            e("$u = ", raiz(frac("P × Z", k(t, "cos φ")))),
            e("$u = ", raiz(frac("P × R", k(t, "cos^2 φ")))),
            e("$u = ", frac("S", k(t, "I"))),
            e("$u = ", raiz(if (t) frac("S × Z", raiz("3")) else e("S × Z"))),
            e("$u = ", raiz(frac("S × R", k(t, "cos φ")))),
            e("$u = ", frac("Q", k(t, "I × $sen φ"))),
            e("$u = ", raiz(frac("Q × Z", k(t, "$sen φ")))),
            e("$u = ", raiz(frac("Q × R", k(t, "$sen φ × cos φ")))),
        )
    }

    private fun potenciaActiva() = ohm(
        cc = { u -> listOf(e("P = $u × I"), e("P = R × I^2"), e("P = ", frac("${u}^2", "R"))) },
    ) { u, t ->
        listOf(
            e("P = ", k(t, "$u × I × cos φ")),
            e("P = ", k(t, "Z × I^2 × cos φ")),
            e("P = ", k(t, "R × I^2")),
            e("P = ", frac(k(t, "${u}^2 × cos φ"), "Z")),
            e("P = ", frac(k(t, "${u}^2 × cos^2 φ"), "R")),
            e("P = S × cos φ"),
            e("P = ", frac("Q", "tan φ")),
            e("P = ", raiz("S^2 − Q^2")),
        )
    }

    private fun potenciaAparente() = ohm(
        cc = { u -> listOf(e("S = $u × I")) },
    ) { u, t ->
        listOf(
            e("S = ", k(t, "$u × I")),
            e("S = ", k(t, "Z × I^2")),
            e("S = ", frac(k(t, "R × I^2"), "cos φ")),
            e("S = ", frac(k(t, "${u}^2"), "Z")),
            e("S = ", frac(k(t, "${u}^2 × cos φ"), "R")),
            e("S = ", frac("P", "cos φ")),
            e("S = ", frac("Q", "$sen φ")),
            e("S = ", raiz("P^2 + Q^2")),
        )
    }

    private fun potenciaReactiva() = ohm(cc = null) { u, t ->
        listOf(
            e("Q = ", k(t, "$u × I × $sen φ")),
            e("Q = ", k(t, "Z × I^2 × $sen φ")),
            e("Q = ", k(t, "R × I^2 × tan φ")),
            e("Q = ", frac(k(t, "${u}^2 × $sen φ"), "Z")),
            e("Q = ", frac(k(t, "${u}^2 × $sen φ × cos φ"), "R")),
            e("Q = S × $sen φ"),
            e("Q = P × tan φ"),
            e("Q = ", raiz("S^2 − P^2")),
        )
    }

    private fun factorPotencia() = ohm(cc = null) { u, t ->
        listOf(
            e("cos φ = ", frac("P", k(t, "$u × I"))),
            e("cos φ = ", frac("P", "S")),
            e("cos φ = ", frac("R", "Z")),
            e("cos φ = ", frac("P", raiz("P^2 + Q^2"))),
            e("cos φ = ", frac(raiz("S^2 − Q^2"), "S")),
            e("cos φ = ", frac("P × Z", k(t, "${u}^2"))),
            e("cos φ = ", frac("P", k(t, "Z × I^2"))),
        )
    }

    private fun resistencia() = ohm(
        cc = { u -> listOf(e("R = ", frac(u, "I")), e("R = ", frac("P", "I^2")), e("R = ", frac("${u}^2", "P"))) },
    ) { u, t ->
        listOf(
            e("R = ", frac("$u × cos φ", "I")),
            e("R = ", frac("P", k(t, "I^2"))),
            e("R = ", frac(k(t, "${u}^2 × cos^2 φ"), "P")),
            e("R = ", frac("S × cos φ", k(t, "I^2"))),
            e("R = ", frac(k(t, "${u}^2 × cos φ"), "S")),
            e("R = ", frac("Q × cos φ", k(t, "I^2 × $sen φ"))),
            e("R = ", frac(k(t, "${u}^2 × $sen φ × cos φ"), "Q")),
            e("R = Z × cos φ"),
        )
    }

    private fun impedancia() = ohm(cc = null) { u, t ->
        listOf(
            e("Z = ", frac(u, "I")),
            e("Z = ", frac(k(t, "${u}^2 × cos φ"), "P")),
            e("Z = ", frac("P", k(t, "I^2 × cos φ"))),
            e("Z = ", frac(k(t, "${u}^2"), "S")),
            e("Z = ", frac("S", k(t, "I^2"))),
            e("Z = ", frac(k(t, "${u}^2 × $sen φ"), "Q")),
            e("Z = ", frac("Q", k(t, "I^2 × $sen φ"))),
            e("Z = ", frac("R", "cos φ")),
        )
    }

    private fun caida(): Ficha {
        val porcentaje = e("ΔU% = ", frac("ΔU", "U"), " × 100")
        val temperatura = e("R_T = R_ref × (1 + α × (T − T_ref))")
        return Ficha(
            tarjetas = listOf(
                TarjetaFicha(R.string.sistema_continua, listOf(e("ΔU = ", frac("2 × I × R × L", "n")))),
                TarjetaFicha(
                    R.string.sistema_mono_bifasica,
                    listOf(e("ΔU = ", frac("2 × I × (R × cos φ + X × $sen φ) × L", "n"))),
                ),
                TarjetaFicha(
                    R.string.sistema_trifasica,
                    listOf(e("ΔU = ", frac(e(raiz("3"), " × I × (R × cos φ + X × $sen φ) × L"), "n"))),
                ),
                TarjetaFicha(R.string.ficha_comunes, listOf(porcentaje, e("U_carga = U − ΔU"), temperatura)),
            ),
            leyenda = listOf(
                Simbolo("ΔU", R.string.sim_caida, "V"),
                Simbolo("U", R.string.sim_tension_alimentacion, "V"),
                Simbolo("I", R.string.sim_corriente, "A"),
                Simbolo("R", R.string.sim_resistencia_linea, "Ω/km"),
                Simbolo("X", R.string.sim_reactancia_linea, "Ω/km"),
                Simbolo("L", R.string.sim_longitud, "km"),
                Simbolo("n", R.string.sim_paralelos),
                Simbolo("φ", R.string.sim_desfase, "°"),
                Simbolo("α", R.string.sim_alfa, "1/°C"),
                Simbolo("T", R.string.sim_temperatura, "°C"),
            ),
            notas = listOf(R.string.nota_caida_tablas),
        )
    }

    /** Factor de la línea según el sistema: 2 (ida y vuelta) o √3 en trifásica. */
    private fun factorLinea(tri: Boolean, resto: String): Expr = if (tri) e(raiz("3"), " × $resto") else e("2 × $resto")

    private val leyendaLinea = listOf(
        Simbolo("I", R.string.sim_corriente, "A"),
        Simbolo("R", R.string.sim_resistencia_linea, "Ω/km"),
        Simbolo("X", R.string.sim_reactancia_linea, "Ω/km"),
        Simbolo("n", R.string.sim_paralelos),
        Simbolo("φ", R.string.sim_desfase, "°"),
    )

    /** N cargas iguales a la misma distancia: equivale a la carga total a (N + 1)/(2N) del largo. */
    private fun caidaDistribuida(): Ficha {
        val reparto = frac("N + 1", "2")
        return Ficha(
            tarjetas = listOf(
                TarjetaFicha(R.string.sistema_continua, listOf(e("ΔU = ", frac("2 × I × R × L", "n"), " × ", reparto))),
                TarjetaFicha(
                    R.string.sistema_mono_bifasica,
                    listOf(e("ΔU = ", frac("2 × I × (R × cos φ + X × $sen φ) × L", "n"), " × ", reparto)),
                ),
                TarjetaFicha(
                    R.string.sistema_trifasica,
                    listOf(e("ΔU = ", frac(e(raiz("3"), " × I × (R × cos φ + X × $sen φ) × L"), "n"), " × ", reparto)),
                ),
                TarjetaFicha(R.string.ficha_comunes, listOf(e("d = ", frac("L", "N")), e("I_inicio = N × I"))),
            ),
            leyenda = listOf(Simbolo("ΔU", R.string.sim_caida, "V")) + leyendaLinea + listOf(
                Simbolo("L", R.string.sim_longitud_total, "km"),
                Simbolo("N", R.string.sim_numero_cargas),
                Simbolo("d", R.string.sim_distancia_cargas, "km"),
            ),
            notas = listOf(R.string.nota_caida_distribuidas, R.string.nota_caida_tablas),
        )
    }

    private fun longitudMaxima(): Ficha {
        fun formula(tri: Boolean, alterna: Boolean): Expr {
            val impedancia = if (alterna) "I × (R × cos φ + X × $sen φ)" else "I × R"
            return e("L_max = ", frac("ΔU_max × n", factorLinea(tri, impedancia)))
        }
        return Ficha(
            tarjetas = listOf(
                TarjetaFicha(R.string.sistema_continua, listOf(formula(tri = false, alterna = false))),
                TarjetaFicha(R.string.sistema_mono_bifasica, listOf(formula(tri = false, alterna = true))),
                TarjetaFicha(R.string.sistema_trifasica, listOf(formula(tri = true, alterna = true))),
                TarjetaFicha(R.string.ficha_comunes, listOf(e("ΔU_max = ", frac("ΔU% × U", "100")))),
            ),
            leyenda = listOf(
                Simbolo("L_max", R.string.sim_longitud_maxima, "km"),
                Simbolo("ΔU_max", R.string.sim_caida_maxima, "V"),
                Simbolo("U", R.string.sim_tension_alimentacion, "V"),
            ) + leyendaLinea,
            notas = listOf(R.string.nota_caida_tablas),
        )
    }

    private val leyendaMotor = listOf(
        Simbolo("P", R.string.sim_potencia_eje, "W"),
        Simbolo("U", R.string.sim_tension_alimentacion, "V"),
        Simbolo("I", R.string.sim_corriente, "A"),
        Simbolo("cos φ", R.string.sim_factor_potencia),
        Simbolo("η", R.string.sim_rendimiento),
    )

    /** Fórmula de motor por sistema: en continua sin cos φ, en trifásica con √3. */
    private fun motor(cc: String, ca: (tri: Boolean) -> Expr): Ficha = Ficha(
        listOf(
            TarjetaFicha(R.string.sistema_continua, listOf(e(cc))),
            TarjetaFicha(R.string.sistema_monofasica, listOf(ca(false))),
            TarjetaFicha(R.string.sistema_trifasica, listOf(ca(true))),
        ),
        leyendaMotor,
    )

    private fun corrienteMotor() = motor("I = P / (U × η)") { t -> e("I = ", frac("P", k(t, "U × cos φ × η"))) }
    private fun potenciaMotor() = motor("P = U × I × η") { t -> e("P = ", k(t, "U × I × cos φ × η")) }
    private fun tensionMotor() = motor("U = P / (I × η)") { t -> e("U = ", frac("P", k(t, "I × cos φ × η"))) }
    private fun rendimientoMotor() = motor("η = P / (U × I)") { t -> e("η = ", frac("P", k(t, "U × I × cos φ"))) }
    private fun fpMotor() = Ficha(
        listOf(
            TarjetaFicha(R.string.sistema_monofasica, listOf(e("cos φ = ", frac("P", "U × I × η")))),
            TarjetaFicha(R.string.sistema_trifasica, listOf(e("cos φ = ", frac("P", k(true, "U × I × η"))))),
        ),
        leyendaMotor,
    )

    private val leyendaVelocidad = listOf(
        Simbolo("n_s", R.string.sim_velocidad_sincrona, "rpm"),
        Simbolo("n", R.string.sim_velocidad_rotor, "rpm"),
        Simbolo("f", R.string.sim_frecuencia, "Hz"),
        Simbolo("p", R.string.sim_polos),
        Simbolo("s", R.string.sim_deslizamiento),
    )

    private fun velocidadMotor() = Ficha(
        listOf(TarjetaFicha(R.string.ficha_comunes, listOf(e("n_s = ", frac("120 × f", "p")), e("n = n_s × (1 − s)")))),
        leyendaVelocidad,
    )

    private fun deslizamientoMotor() = Ficha(
        listOf(TarjetaFicha(R.string.ficha_comunes, listOf(e("n_s = ", frac("120 × f", "p")), e("s = ", frac("n_s − n", "n_s"), " × 100")))),
        leyendaVelocidad,
    )

    private fun parMotor() = Ficha(
        listOf(TarjetaFicha(R.string.ficha_comunes, listOf(e("T = ", frac("60 × P", "2π × n")), e("P = ", frac("2π × n × T", "60"))))),
        listOf(
            Simbolo("T", R.string.sim_par, "N·m"),
            Simbolo("P", R.string.sim_potencia_eje, "W"),
            Simbolo("n", R.string.sim_velocidad_rotor, "rpm"),
        ),
    )

    private fun relacionTransformador() = Ficha(
        listOf(TarjetaFicha(R.string.ficha_comunes, listOf(e("a = ", frac("U_1", "U_2"), " = ", frac("N_1", "N_2"), " = ", frac("I_2", "I_1"))))),
        listOf(
            Simbolo("a", R.string.sim_relacion),
            Simbolo("U_1, U_2", R.string.sim_tensiones_devanados, "V"),
            Simbolo("N_1, N_2", R.string.sim_espiras),
            Simbolo("I_1, I_2", R.string.sim_corrientes_devanados, "A"),
        ),
    )

    private fun simple(formulas: List<Expr>, vararg leyenda: Simbolo) =
        Ficha(listOf(TarjetaFicha(R.string.ficha_comunes, formulas)), leyenda.toList())

    private fun reactanciaF() = simple(
        listOf(e("X_L = 2π × f × L"), e("X_C = ", frac("1", "2π × f × C"))),
        Simbolo("X_L, X_C", R.string.sim_reactancia, "Ω"), Simbolo("f", R.string.sim_frecuencia, "Hz"),
        Simbolo("L", R.string.sim_inductancia, "H"), Simbolo("C", R.string.sim_capacidad, "F"),
    )

    private fun impedanciaRx() = simple(
        listOf(e("Z = ", raiz("R^2 + X^2")), e("φ = arctan(X / R)"), e("cos φ = ", frac("R", "Z"))),
        Simbolo("Z", R.string.sim_impedancia, "Ω"), Simbolo("R", R.string.sim_resistencia, "Ω"),
        Simbolo("X", R.string.sim_reactancia, "Ω"), Simbolo("φ", R.string.sim_desfase, "°"),
    )

    private fun jouleF() = simple(
        listOf(e("Q = I^2 × R × t")),
        Simbolo("Q", R.string.sim_calor, "J"), Simbolo("I", R.string.sim_corriente, "A"),
        Simbolo("R", R.string.sim_resistencia, "Ω"), Simbolo("t", R.string.sim_tiempo, "s"),
    )

    private fun sumaResistenciasF() = Ficha(
        listOf(
            TarjetaFicha(R.string.conexion_serie, listOf(e("R_eq = R_1 + R_2 + … + R_n"))),
            TarjetaFicha(R.string.conexion_paralelo, listOf(e(frac("1", "R_eq"), " = ", frac("1", "R_1"), " + ", frac("1", "R_2"), " + …"))),
        ),
        listOf(Simbolo("R_eq", R.string.sim_equivalente, "Ω")),
    )

    private fun sumaCondensadoresF() = Ficha(
        listOf(
            TarjetaFicha(R.string.conexion_serie, listOf(e(frac("1", "C_eq"), " = ", frac("1", "C_1"), " + ", frac("1", "C_2"), " + …"))),
            TarjetaFicha(R.string.conexion_paralelo, listOf(e("C_eq = C_1 + C_2 + … + C_n"))),
        ),
        listOf(Simbolo("C_eq", R.string.sim_equivalente, "F")),
    )

    private fun resonanciaF() = simple(
        listOf(e("f_0 = ", frac("1", e("2π × ", raiz("L × C"))))),
        Simbolo("f_0", R.string.sim_frecuencia, "Hz"), Simbolo("L", R.string.sim_inductancia, "H"), Simbolo("C", R.string.sim_capacidad, "F"),
    )

    private fun condensadorF() = simple(
        listOf(e("Q = U^2 × 2π × f × C")),
        Simbolo("Q", R.string.sim_potencia_reactiva, "var"), Simbolo("U", R.string.sim_tension_alimentacion, "V"),
        Simbolo("f", R.string.sim_frecuencia, "Hz"), Simbolo("C", R.string.sim_capacidad, "F"),
    )

    private fun divisorTensionF() = simple(
        listOf(e("U_out = U_in × ", frac("R_2", "R_1 + R_2"))),
        Simbolo("U_in", R.string.sim_tension_entrada, "V"), Simbolo("U_out", R.string.sim_tension_salida, "V"),
    )

    private fun divisorCorrienteF() = simple(
        listOf(e("I_1 = I × ", frac("R_2", "R_1 + R_2")), e("I_2 = I × ", frac("R_1", "R_1 + R_2"))),
        Simbolo("I", R.string.sim_corriente, "A"),
    )

    private fun zenerF() = simple(
        listOf(e("R = ", frac("U_in − U_Z", "I_Z + I_L")), e("P_R = R × (I_Z + I_L)^2")),
        Simbolo("U_in", R.string.sim_tension_entrada, "V"), Simbolo("U_Z", R.string.sim_tension_zener, "V"),
        Simbolo("I_Z", R.string.sim_corriente_zener, "A"), Simbolo("I_L", R.string.sim_corriente_carga, "A"),
    )

    private fun reducirTensionF() = simple(
        listOf(e("R = ", frac("U_in − U_out", "I")), e("P_R = R × I^2"), e("LED: R = ", frac("U_in − n × U_f", "I"))),
        Simbolo("U_in", R.string.sim_tension_entrada, "V"), Simbolo("U_out", R.string.sim_tension_salida, "V"),
        Simbolo("I", R.string.sim_corriente_carga, "A"),
    )

    private fun antenaF() = simple(
        listOf(e("λ = ", frac("c × k", "f")), e("L_dipolo = ", frac("λ", "2")), e("L_monopolo = ", frac("λ", "4"))),
        Simbolo("λ", R.string.sim_longitud_onda, "m"), Simbolo("c", R.string.sim_velocidad_luz, "m/s"),
        Simbolo("k", R.string.sim_factor_velocidad), Simbolo("f", R.string.sim_frecuencia, "Hz"),
    )

    private fun temperaturaF() = simple(
        listOf(e("°F = °C × ", frac("9", "5"), " + 32"), e("K = °C + 273,15")),
        Simbolo("°C", R.string.sim_temperatura_c), Simbolo("°F", R.string.sim_temperatura_f), Simbolo("K", R.string.sim_temperatura_k),
    )

    private fun amplitudF() = simple(
        listOf(e("U_p = ", raiz("2"), " × U_ef"), e("U_pp = 2 × U_p"), e("U_med = ", frac("2 × U_p", "π"))),
        Simbolo("U_p", R.string.sim_pico, "V"), Simbolo("U_ef", R.string.sim_eficaz, "V"),
    )

    private fun trigonometricaF() = simple(
        listOf(e("tan φ = ", frac("$sen φ", "cos φ")), e("$sen φ = ", raiz("1 − cos^2 φ")), e("φ = arccos(cos φ)")),
        Simbolo("φ", R.string.sim_desfase, "°"),
    )

    private fun ahKwhF() = simple(
        listOf(e("E = ", frac("C × U", "1000")), e("C = ", frac("1000 × E", "U"))),
        Simbolo("E", R.string.sim_energia, "kWh"), Simbolo("C", R.string.sim_capacidad_ah, "Ah"), Simbolo("U", R.string.sim_tension_alimentacion, "V"),
    )

    private fun varUfF() = Ficha(
        listOf(
            TarjetaFicha(R.string.conexion_estrella, listOf(e("C = ", frac("Q", "2π × f × U^2")))),
            TarjetaFicha(R.string.conexion_triangulo, listOf(e("C = ", frac("Q", "3 × 2π × f × U^2")))),
        ),
        listOf(
            Simbolo("C", R.string.sim_capacidad, "F"), Simbolo("Q", R.string.sim_potencia_reactiva, "var"),
            Simbolo("U", R.string.sim_tension_ff, "V"), Simbolo("f", R.string.sim_frecuencia, "Hz"),
        ),
    )

    private fun rpmF() = simple(
        listOf(e("ω = ", frac("2π × n", "60")), e("v = ω × ", frac("D", "2"))),
        Simbolo("ω", R.string.sim_velocidad_angular, "rad/s"), Simbolo("n", R.string.sim_velocidad_rotor, "rpm"),
        Simbolo("D", R.string.sim_diametro, "m"), Simbolo("v", R.string.sim_velocidad_tangencial, "m/s"),
    )

    private fun frecuenciaAngularF() = simple(
        listOf(e("ω = 2π × f"), e("T = ", frac("1", "f"))),
        Simbolo("ω", R.string.sim_velocidad_angular, "rad/s"), Simbolo("f", R.string.sim_frecuencia, "Hz"),
    )

    private fun trianguloEstrellaF() = Ficha(
        listOf(
            TarjetaFicha(R.string.triangulo_a_estrella, listOf(e("R_a = ", frac("R_ab × R_ca", "R_ab + R_bc + R_ca")))),
            TarjetaFicha(R.string.estrella_a_triangulo, listOf(e("R_ab = ", frac("R_a R_b + R_b R_c + R_c R_a", "R_c")))),
        ),
        listOf(Simbolo("R", R.string.sim_resistencia, "Ω")),
    )

    private fun seccionF() = simple(
        listOf(e("d = 0,127 × 92^((36 − n) / 39)"), e("S = ", frac("π × d^2", "4")), e("1 kcmil = 0,5067 mm²")),
        Simbolo("n", R.string.sim_calibre_awg), Simbolo("d", R.string.sim_diametro, "mm"),
    )

    private fun anguloF() = simple(
        listOf(e("rad = ° × ", frac("π", "180")), e("grad = ° × ", frac("10", "9"))),
        Simbolo("°", R.string.sim_angulo_grados), Simbolo("rad", R.string.sim_angulo_rad),
    )

    private fun resistenciaCableF() = simple(
        listOf(e("R_20 = ", frac("ρ × L", "S × n")), temperaturaR()),
        Simbolo("ρ", R.string.sim_resistividad, "Ω·mm²/m"), Simbolo("L", R.string.sim_longitud, "m"),
        Simbolo("S", R.string.sim_seccion, "mm²"), Simbolo("n", R.string.sim_paralelos),
        Simbolo("α", R.string.sim_alfa, "1/°C"), Simbolo("T", R.string.sim_temperatura, "°C"),
    )

    private fun temperaturaR() = e("R_T = R_20 × (1 + α × (T − 20))")

    private fun perdidasCableF() = simple(
        listOf(e("P = k × I^2 × R")),
        Simbolo("P", R.string.sim_perdidas, "W"), Simbolo("k", R.string.sim_conductores_activos),
        Simbolo("I", R.string.sim_corriente, "A"), Simbolo("R", R.string.sim_resistencia, "Ω"),
    )

    private fun temperaturaCableF() = simple(
        listOf(e("T = T_a + (T_max − T_a) × ", e("(", frac("I", "I_z"), ")^2"))),
        Simbolo("T_a", R.string.sim_temperatura_ambiente, "°C"), Simbolo("T_max", R.string.sim_temperatura_maxima, "°C"),
        Simbolo("I", R.string.sim_corriente, "A"), Simbolo("I_z", R.string.sim_corriente_admisible, "A"),
    )

    private fun corrienteEmpleoF() = Ficha(
        listOf(
            TarjetaFicha(R.string.sistema_continua, listOf(e("I_b = ", frac("P × k_u × k_c", "U")))),
            TarjetaFicha(R.string.sistema_monofasica, listOf(e("I_b = ", frac("P × k_u × k_c", "U × cos φ")))),
            TarjetaFicha(R.string.sistema_trifasica, listOf(e("I_b = ", frac("P × k_u × k_c", k(true, "U × cos φ"))))),
        ),
        listOf(
            Simbolo("P", R.string.sim_potencia_activa, "W"), Simbolo("k_u", R.string.sim_factor_utilizacion),
            Simbolo("k_c", R.string.sim_factor_simultaneidad), Simbolo("U", R.string.sim_tension_ff, "V"),
        ),
    )

    private fun neutroF() = simple(
        listOf(e("I_N = ", raiz("I_1^2 + I_2^2 + I_3^2 − I_1 I_2 − I_2 I_3 − I_3 I_1"))),
        Simbolo("I_1, I_2, I_3", R.string.sim_corrientes_fase, "A"),
    )

    private val simK = Simbolo("k", R.string.sim_k)
    private val simS = Simbolo("S", R.string.sim_seccion, "mm²")

    private fun energiaEspecificaF() = simple(listOf(e("I^2 t ≤ k^2 × S^2"), e("t_max = ", e("(", frac("k × S", "I"), ")^2"))), simK, simS, Simbolo("I", R.string.sim_icc, "A"))

    private fun proteccionCableF() = simple(
        listOf(e("S ≥ ", frac(e("I_cc × ", raiz("t")), "k"))),
        simS, Simbolo("I_cc", R.string.sim_icc, "A"), Simbolo("t", R.string.sim_tiempo_despeje, "s"), simK,
    )

    private fun dispositivoF() = simple(
        listOf(e("I_b ≤ I_n ≤ I_z")),
        Simbolo("I_b", R.string.sim_ib, "A"), Simbolo("I_n", R.string.sim_in, "A"), Simbolo("I_z", R.string.sim_corriente_admisible, "A"),
    )

    private fun iccTrafoF() = simple(
        listOf(e("Z_t = ", frac("u_cc × U^2", "S")), e("Z_r = ", frac("U^2", "S_cc")), e("I_cc = ", frac("U", e(raiz("3"), " × (Z_r + Z_t)")))),
        Simbolo("S", R.string.sim_potencia_trafo, "VA"), Simbolo("u_cc", R.string.sim_ucc), Simbolo("S_cc", R.string.sim_scc, "VA"),
        Simbolo("U", R.string.sim_tension_ff, "V"),
    )

    private fun iccLineaF() = simple(
        listOf(e("Z_0 = ", frac("U", e(raiz("3"), " × I_cc0"))), e("I_cc = ", frac("U", e(raiz("3"), " × |Z_0 + R + jX|")))),
        Simbolo("Z_0", R.string.sim_impedancia_origen, "Ω"), Simbolo("R", R.string.sim_resistencia, "Ω"), Simbolo("X", R.string.sim_reactancia, "Ω"),
    )

    private fun iccMinimaF() = simple(
        listOf(e("Z_0 = ", frac("U_0", "I_cc0")), e("I_cc,min = ", frac("U_0", "|Z_0 + 2 × (R + jX)|"))),
        Simbolo("U_0", R.string.sim_tension_fn, "V"), Simbolo("Z_0", R.string.sim_impedancia_origen, "Ω"),
    )

    private fun largoMaximoCcF() = simple(
        listOf(e("|Z_0 + 2 × (r + jx) × L| = ", frac("U_0", "I_a"))),
        Simbolo("I_a", R.string.sim_corriente_disparo, "A"), Simbolo("r, x", R.string.sim_resistencia_linea, "Ω/km"), Simbolo("L", R.string.sim_longitud, "km"),
    )

    private val constructores: Map<String, () -> Ficha> = mapOf(
        "FormulaTabEnergiaSpecifica" to ::energiaEspecificaF,
        "FormulaProtezioneCavoCortocircuito" to ::proteccionCableF,
        "FormulaDispositivoProtezione" to ::dispositivoF,
        "FormulaCorrenteCortoCircuitoTrasf" to ::iccTrafoF,
        "FormulaCorrenteCortoCircuito" to ::iccLineaF,
        "FormulaCorrenteCortoCircuitoMin" to ::iccMinimaF,
        "FormulaLunghezzaCavoCortocircuito" to ::largoMaximoCcF,
        "FormulaResistenzaCavo" to ::resistenciaCableF,
        "FormulaPerditaPotenzaCavo" to ::perdidasCableF,
        "FormulaTemperaturaCavo" to ::temperaturaCableF,
        "FormulaCorrenteImpiego" to ::corrienteEmpleoF,
        "FormulaCorrenteNeutro" to ::neutroF,
        "FormulaConversioneTemperatura" to ::temperaturaF,
        "FormulaAmplitude" to ::amplitudF,
        "FormulaFunzioniTrigonometriche" to ::trigonometricaF,
        "FormulaConversioneAhKwh" to ::ahKwhF,
        "FormulaConversioneVarUf" to ::varUfF,
        "FormulaConversioneRpm" to ::rpmF,
        "FormulaFrequenzaAngolare" to ::frecuenciaAngularF,
        "FormulaTriangoloStella" to ::trianguloEstrellaF,
        "FormulaConversioneSezione" to ::seccionF,
        "FormulaConversioneAngolo" to ::anguloF,
        "FormulaReattanza" to ::reactanciaF,
        "FormulaImpedenzaDaResistenzaEReattanza" to ::impedanciaRx,
        "FormulaEffettoJoule" to ::jouleF,
        "FormulaSommaResistenze" to ::sumaResistenciasF,
        "FormulaSommaCondensatori" to ::sumaCondensadoresF,
        "FormulaFrequenzaRisonanza" to ::resonanciaF,
        "FormulaPotenzaResaCondensatore" to ::condensadorF,
        "FormulaPartitoreTensione" to ::divisorTensionF,
        "FormulaPartitoreCorrente" to ::divisorCorrienteF,
        "FormulaStabilizzatoreTensioneZener" to ::zenerF,
        "FormulaResistenzaRidurreTensione" to ::reducirTensionF,
        "FormulaLunghezzaAntenna" to ::antenaF,
        "FormulaCorrenteMotore" to ::corrienteMotor,
        "FormulaPotenzaMotore" to ::potenciaMotor,
        "FormulaTensioneMotore" to ::tensionMotor,
        "FormulaFattorePotenzaMotore" to ::fpMotor,
        "FormulaRendimentoMotore" to ::rendimientoMotor,
        "FormulaVelocitaMotore" to ::velocidadMotor,
        "FormulaScorrimentoMotore" to ::deslizamientoMotor,
        "FormulaCoppiaMassima" to ::parMotor,
        "FormulaPrimarioSecondarioTrasformatore" to ::relacionTransformador,
        "FormulaCorrente" to ::corriente,
        "FormulaTensione" to ::tension,
        "FormulaPotenzaAttiva" to ::potenciaActiva,
        "FormulaPotenzaApparente" to ::potenciaAparente,
        "FormulaPotenzaReattiva" to ::potenciaReactiva,
        "FormulaFattorePotenza" to ::factorPotencia,
        "FormulaResistenza" to ::resistencia,
        "FormulaImpedenza" to ::impedancia,
        "FormulaCaduta" to ::caida,
        "FormulaCadutaCarichiDistribuiti" to ::caidaDistribuida,
        "FormulaLunghezzaCavoCaduta" to ::longitudMaxima,
    )

    fun existe(id: String?) = id != null && id in constructores

    /** Se arma al pedirla para que los textos (sen/sin) sigan el idioma actual. */
    fun ficha(id: String): Ficha? = constructores[id]?.invoke()
}
