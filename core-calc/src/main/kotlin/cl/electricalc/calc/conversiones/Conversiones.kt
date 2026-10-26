package cl.electricalc.calc.conversiones

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

private fun positivo(vararg v: Double) {
    if (v.any { !(it > 0.0) || it.isInfinite() }) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
}

/** Unidad lineal: [factor] es cuántas unidades base vale una de esta unidad. */
class UnidadLineal(val simbolo: String, val factor: Double)

/** Tablas de unidades con factores exactos por definición. */
object Unidades {
    val longitud = listOf(
        UnidadLineal("m", 1.0), UnidadLineal("km", 1e3), UnidadLineal("cm", 1e-2), UnidadLineal("mm", 1e-3),
        UnidadLineal("in", 0.0254), UnidadLineal("ft", 0.3048), UnidadLineal("yd", 0.9144), UnidadLineal("mi", 1609.344),
    )
    val potencia = listOf(
        UnidadLineal("W", 1.0), UnidadLineal("kW", 1e3), UnidadLineal("MW", 1e6),
        UnidadLineal("HP", 745.69987158227022), UnidadLineal("CV", 735.49875),
        UnidadLineal("BTU/h", 1055.05585262 / 3600), UnidadLineal("kcal/h", 4184.0 / 3600),
    )
    val energia = listOf(
        UnidadLineal("J", 1.0), UnidadLineal("kJ", 1e3), UnidadLineal("Wh", 3600.0), UnidadLineal("kWh", 3.6e6),
        UnidadLineal("MWh", 3.6e9), UnidadLineal("cal", 4.184), UnidadLineal("kcal", 4184.0), UnidadLineal("BTU", 1055.05585262),
    )
    val presion = listOf(
        UnidadLineal("Pa", 1.0), UnidadLineal("kPa", 1e3), UnidadLineal("MPa", 1e6), UnidadLineal("bar", 1e5),
        UnidadLineal("mbar", 100.0), UnidadLineal("atm", 101325.0), UnidadLineal("psi", 6894.757293168361),
        UnidadLineal("mmHg", 133.322387415), UnidadLineal("kgf/cm²", 98066.5),
    )
    val campoMagnetico = listOf(UnidadLineal("T", 1.0), UnidadLineal("mT", 1e-3), UnidadLineal("µT", 1e-6), UnidadLineal("G", 1e-4), UnidadLineal("mG", 1e-7))
    val par = listOf(
        UnidadLineal("N·m", 1.0), UnidadLineal("kN·m", 1e3), UnidadLineal("kgf·m", 9.80665),
        UnidadLineal("lbf·ft", 1.3558179483314004), UnidadLineal("lbf·in", 0.1129848290276167),
    )
    val datos = listOf(
        UnidadLineal("bit", 0.125), UnidadLineal("B", 1.0), UnidadLineal("kB", 1e3), UnidadLineal("MB", 1e6),
        UnidadLineal("GB", 1e9), UnidadLineal("TB", 1e12), UnidadLineal("KiB", 1024.0), UnidadLineal("MiB", 1024.0.pow(2)),
        UnidadLineal("GiB", 1024.0.pow(3)), UnidadLineal("TiB", 1024.0.pow(4)),
    )
    val angulo = listOf(UnidadLineal("°", PI / 180), UnidadLineal("rad", 1.0), UnidadLineal("grad", PI / 200), UnidadLineal("rev", 2 * PI))

    fun convertir(valor: Double, de: UnidadLineal, a: UnidadLineal): Double {
        if (!valor.isFinite()) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return valor * de.factor / a.factor
    }
}

object Temperatura {
    private const val CERO_K = 273.15

    fun aCelsius(valor: Double, escala: Int): Double = when (escala) {
        0 -> valor
        1 -> (valor - 32) * 5 / 9
        2 -> valor - CERO_K
        else -> throw IllegalArgumentException()
    }.also { if (it < -CERO_K - 1e-9) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO) }

    fun fahrenheit(c: Double) = c * 9 / 5 + 32
    fun kelvin(c: Double) = c + CERO_K
}

/** Valores de una onda senoidal. */
object Senoidal {
    fun picoDeEficaz(eficaz: Double) = eficaz * sqrt(2.0)
    fun eficazDePico(pico: Double) = pico / sqrt(2.0)
    fun medioDePico(pico: Double) = 2 * pico / PI
}

object Conversiones {
    /** Ah a kWh con la tensión nominal de la batería. */
    fun kwh(ah: Double, tension: Double): Double {
        positivo(ah, tension)
        return ah * tension / 1000
    }

    fun ah(kwh: Double, tension: Double): Double {
        positivo(kwh, tension)
        return kwh * 1000 / tension
    }

    /** Capacidad por fase de un banco trifásico de Q (var) a tensión de línea U: estrella C = Q / (ωU²), triángulo C = Q / (3ωU²). */
    fun capacidadBanco(q: Double, tensionLinea: Double, frecuencia: Double, triangulo: Boolean): Double {
        positivo(q, tensionLinea, frecuencia)
        val omega = 2 * PI * frecuencia
        return q / ((if (triangulo) 3 else 1) * omega * tensionLinea * tensionLinea)
    }

    fun reactivaBanco(capacidad: Double, tensionLinea: Double, frecuencia: Double, triangulo: Boolean): Double {
        positivo(capacidad, tensionLinea, frecuencia)
        return capacidad * (if (triangulo) 3 else 1) * 2 * PI * frecuencia * tensionLinea * tensionLinea
    }

    fun radPorSegundo(rpm: Double) = rpm * 2 * PI / 60

    /** Velocidad tangencial en m/s para un diámetro en m. */
    fun velocidadTangencial(rpm: Double, diametro: Double): Double {
        positivo(diametro)
        return radPorSegundo(rpm) * diametro / 2
    }

    /** Triángulo a estrella: Ra = Rab·Rca / (Rab + Rbc + Rca), etc. Devuelve (Ra, Rb, Rc). */
    fun trianguloAEstrella(rab: Double, rbc: Double, rca: Double): Triple<Double, Double, Double> {
        positivo(rab, rbc, rca)
        val s = rab + rbc + rca
        return Triple(rab * rca / s, rab * rbc / s, rbc * rca / s)
    }

    /** Estrella a triángulo: Rab = (RaRb + RbRc + RcRa) / Rc, etc. Devuelve (Rab, Rbc, Rca). */
    fun estrellaATriangulo(ra: Double, rb: Double, rc: Double): Triple<Double, Double, Double> {
        positivo(ra, rb, rc)
        val p = ra * rb + rb * rc + rc * ra
        return Triple(p / rc, p / ra, p / rb)
    }
}

/** Calibres AWG y kcmil por su definición (ASTM B258). */
object Awg {
    /** Diámetro en mm del calibre AWG [n]; 0, −1, −2, −3 son 1/0, 2/0, 3/0, 4/0. */
    fun diametroMm(n: Int): Double = 0.127 * 92.0.pow((36 - n) / 39.0)

    fun seccionMm2(n: Int): Double = PI / 4 * diametroMm(n).pow(2)

    /** Calibre (posiblemente fraccionario) de una sección en mm². */
    fun calibre(mm2: Double): Double {
        positivo(mm2)
        val d = sqrt(4 * mm2 / PI)
        return 36 - 39 * ln(d / 0.127) / ln(92.0)
    }

    /** 1 kcmil = 1000 mil circulares = 0,506707479 mm². */
    const val MM2_POR_KCMIL = 0.5067074790975

    fun nombre(n: Int): String = if (n > 0) "$n AWG" else "${1 - n}/0 AWG"
}
