package cl.electricalc.calc.motores

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import kotlin.math.PI

private fun positivo(vararg v: Double) {
    if (v.any { !(it > 0.0) || it.isInfinite() }) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
}

private fun fraccion(vararg v: Double) {
    positivo(*v)
    if (v.any { it > 1 }) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
}

/**
 * Relaciones eléctricas de un motor. P es la potencia mecánica en el eje (la de placa); la absorbida
 * es P / η. En continua cos φ vale 1; en trifásica U es la tensión entre fases.
 */
object Motor {
    private fun k(s: SistemaElectrico, cos: Double) = s.factor * (if (s.esAlterna) cos else 1.0)

    fun corriente(s: SistemaElectrico, p: Double, u: Double, cos: Double, eta: Double): Double {
        positivo(p, u); fraccion(cos, eta)
        return p / (k(s, cos) * u * eta)
    }

    fun potencia(s: SistemaElectrico, u: Double, i: Double, cos: Double, eta: Double): Double {
        positivo(u, i); fraccion(cos, eta)
        return k(s, cos) * u * i * eta
    }

    fun tension(s: SistemaElectrico, p: Double, i: Double, cos: Double, eta: Double): Double {
        positivo(p, i); fraccion(cos, eta)
        return p / (k(s, cos) * i * eta)
    }

    /** cos φ = P / (k·U·I·η); un resultado mayor que 1 indica datos incoherentes. */
    fun factorPotencia(s: SistemaElectrico, p: Double, u: Double, i: Double, eta: Double): Double {
        positivo(p, u, i); fraccion(eta)
        val cos = p / (s.factor * u * i * eta)
        if (cos > 1 + 1e-9) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return cos
    }

    /** η = P / (k·U·I·cos φ). */
    fun rendimiento(s: SistemaElectrico, p: Double, u: Double, i: Double, cos: Double): Double {
        positivo(p, u, i); fraccion(cos)
        val eta = p / (k(s, cos) * u * i)
        if (eta > 1 + 1e-9) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return eta
    }

    /** Velocidad de sincronismo en rpm: 120·f / polos. */
    fun velocidadSincrona(frecuencia: Double, polos: Int): Double {
        positivo(frecuencia)
        if (polos < 2 || polos % 2 != 0) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return 120 * frecuencia / polos
    }

    /** Velocidad del rotor con deslizamiento s (0 a 1). */
    fun velocidad(frecuencia: Double, polos: Int, deslizamiento: Double): Double {
        if (deslizamiento < 0 || deslizamiento >= 1) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return velocidadSincrona(frecuencia, polos) * (1 - deslizamiento)
    }

    /** s = (ns − n) / ns. */
    fun deslizamiento(frecuencia: Double, polos: Int, rpm: Double): Double {
        positivo(rpm)
        val ns = velocidadSincrona(frecuencia, polos)
        if (rpm > ns) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return (ns - rpm) / ns
    }

    /** Par en N·m: T = P / ω, con ω = 2π·n / 60. */
    fun par(potencia: Double, rpm: Double): Double {
        positivo(potencia, rpm)
        return potencia / (2 * PI * rpm / 60)
    }

    /** Potencia en W a partir del par y la velocidad. */
    fun potenciaDePar(par: Double, rpm: Double): Double {
        positivo(par, rpm)
        return par * 2 * PI * rpm / 60
    }
}
