package cl.electricalc.calc.electronica

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

private fun positivo(vararg v: Double) {
    if (v.any { !(it > 0.0) || it.isInfinite() }) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
}

/** Reactancias, impedancia serie y resonancia. */
object Reactancia {
    fun inductiva(frecuencia: Double, inductancia: Double): Double {
        positivo(frecuencia, inductancia)
        return 2 * PI * frecuencia * inductancia
    }

    fun capacitiva(frecuencia: Double, capacidad: Double): Double {
        positivo(frecuencia, capacidad)
        return 1 / (2 * PI * frecuencia * capacidad)
    }

    /** Z = √(R² + X²); X positiva inductiva, negativa capacitiva. */
    fun impedancia(r: Double, x: Double): Double {
        if (r < 0 || !r.isFinite() || !x.isFinite()) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return hypot(r, x)
    }

    /** Ángulo de la impedancia en grados. */
    fun angulo(r: Double, x: Double): Double = Math.toDegrees(atan2(x, r))

    /** f₀ = 1 / (2π √(L·C)). */
    fun resonancia(inductancia: Double, capacidad: Double): Double {
        positivo(inductancia, capacidad)
        return 1 / (2 * PI * sqrt(inductancia * capacidad))
    }

    /** Potencia reactiva de un condensador conectado a la tensión U: Q = U² · 2πf · C. */
    fun potenciaCondensador(tension: Double, frecuencia: Double, capacidad: Double): Double {
        positivo(tension, frecuencia, capacidad)
        return tension * tension * 2 * PI * frecuencia * capacidad
    }
}

/** Asociación de componentes. */
object Asociacion {
    /** Suma directa (resistencias en serie, condensadores en paralelo). */
    fun suma(valores: List<Double>): Double {
        if (valores.isEmpty()) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        positivo(*valores.toDoubleArray())
        return valores.sum()
    }

    /** Inversa de la suma de inversas (resistencias en paralelo, condensadores en serie). */
    fun inversa(valores: List<Double>): Double = 1 / suma(valores.map { 1 / it })
}

/** Divisores, resistencias limitadoras y efecto Joule. */
object Circuitos {
    /** Vout = Vin · R2 / (R1 + R2). */
    fun divisorTension(vin: Double, r1: Double, r2: Double): Double {
        positivo(vin, r1, r2)
        return vin * r2 / (r1 + r2)
    }

    /** Corriente por R1 en paralelo con R2: I1 = I · R2 / (R1 + R2). */
    fun divisorCorriente(i: Double, r1: Double, r2: Double): Double {
        positivo(i, r1, r2)
        return i * r2 / (r1 + r2)
    }

    /** Resistencia en serie que baja de [vin] a [vout] con la corriente [i]: R = (Vin − Vout) / I. */
    fun resistenciaSerie(vin: Double, vout: Double, i: Double): Double {
        positivo(vin, vout, i)
        if (vout >= vin) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return (vin - vout) / i
    }

    /** Resistencia para [n] LED en serie de tensión directa [vf]. */
    fun resistenciaLed(fuente: Double, vf: Double, i: Double, n: Int = 1): Double {
        if (n < 1) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return resistenciaSerie(fuente, vf * n, i)
    }

    /** Resistencia de un estabilizador zener: R = (Vin − Vz) / (Iz + Icarga). */
    fun resistenciaZener(vin: Double, vz: Double, iz: Double, icarga: Double): Double {
        if (icarga < 0) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        positivo(iz)
        return resistenciaSerie(vin, vz, iz + icarga)
    }

    /** Calor disipado: Q = I² · R · t (J). */
    fun joule(i: Double, r: Double, segundos: Double): Double {
        positivo(i, r, segundos)
        return i * i * r * segundos
    }

    /** Longitud de onda en m para la frecuencia en Hz y el factor de velocidad del conductor. */
    fun longitudOnda(frecuencia: Double, factorVelocidad: Double = 1.0): Double {
        positivo(frecuencia, factorVelocidad)
        if (factorVelocidad > 1) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return VELOCIDAD_LUZ * factorVelocidad / frecuencia
    }

    /** Escala lineal de una señal entre dos rangos (por ejemplo 4–20 mA a 0–10 bar). */
    fun escalar(x: Double, xMin: Double, xMax: Double, yMin: Double, yMax: Double): Double {
        if (xMax == xMin) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return yMin + (x - xMin) * (yMax - yMin) / (xMax - xMin)
    }

    const val VELOCIDAD_LUZ = 299_792_458.0
}
