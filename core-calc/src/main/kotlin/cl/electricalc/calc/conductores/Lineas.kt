package cl.electricalc.calc.conductores

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import kotlin.math.sqrt

private fun positivo(vararg v: Double) {
    if (v.any { !(it > 0.0) || it.isInfinite() }) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
}

/** Pérdidas, calentamiento y corrientes de una línea. */
object Lineas {
    /** Conductores que llevan la corriente de carga: 2 en continua y monofásica, 3 en trifásica equilibrada. */
    fun conductoresActivos(s: SistemaElectrico): Int = if (s == SistemaElectrico.TRIFASICA) 3 else 2

    /** Pérdida por efecto Joule en la línea, con R la resistencia de un conductor (Ω). */
    fun perdidas(s: SistemaElectrico, corriente: Double, resistenciaConductor: Double): Double {
        positivo(corriente, resistenciaConductor)
        return conductoresActivos(s) * corriente * corriente * resistenciaConductor
    }

    /**
     * Temperatura de régimen del conductor con carga parcial: el calentamiento crece con el cuadrado de
     * la corriente, T = Tamb + (Tmax − Tamb) · (I / Iz)².
     */
    fun temperatura(ambiente: Double, maxima: Double, corriente: Double, admisible: Double): Double {
        positivo(corriente, admisible)
        if (maxima <= ambiente) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        val r = corriente / admisible
        return ambiente + (maxima - ambiente) * r * r
    }

    /** Corriente de empleo Ib = P · ku · kc / (k · U · cos φ). */
    fun corrienteEmpleo(s: SistemaElectrico, potencia: Double, tension: Double, cos: Double, utilizacion: Double, simultaneidad: Double): Double {
        positivo(potencia, tension, cos, utilizacion, simultaneidad)
        if (cos > 1 || utilizacion > 1 || simultaneidad > 1) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        val c = if (s.esAlterna) cos else 1.0
        return potencia * utilizacion * simultaneidad / (s.factor * tension * c)
    }

    /** Corriente del neutro con fases desfasadas 120° y cargas del mismo factor de potencia. */
    fun corrienteNeutro(ia: Double, ib: Double, ic: Double): Double {
        if (listOf(ia, ib, ic).any { it < 0 || !it.isFinite() }) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return sqrt(maxOf(0.0, ia * ia + ib * ib + ic * ic - ia * ib - ib * ic - ic * ia))
    }
}
