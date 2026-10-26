package cl.electricalc.calc.protecciones

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import kotlin.math.hypot
import kotlin.math.sqrt

private fun positivo(vararg v: Double) {
    if (v.any { !(it > 0.0) || it.isInfinite() }) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
}

/**
 * Cortocircuito por el método de impedancias, sin factores de tensión normativos: las corrientes son
 * las que da la tensión nominal sobre la impedancia del bucle.
 */
object Cortocircuito {
    /** Energía específica que soporta el cable: (k·S)², en A²s. */
    fun energiaAdmisible(k: Double, seccionMm2: Double): Double {
        positivo(k, seccionMm2)
        return (k * seccionMm2) * (k * seccionMm2)
    }

    /** Tiempo máximo que el cable soporta la corriente I: t = (k·S / I)². */
    fun tiempoMaximo(k: Double, seccionMm2: Double, corriente: Double): Double {
        positivo(corriente)
        return energiaAdmisible(k, seccionMm2) / (corriente * corriente)
    }

    /** Sección mínima para soportar I durante t: S = I·√t / k. */
    fun seccionMinima(corriente: Double, tiempo: Double, k: Double): Double {
        positivo(corriente, tiempo, k)
        return corriente * sqrt(tiempo) / k
    }

    /** Impedancia de cortocircuito de un transformador: Zt = ucc·U² / S (Ω, referida a U). */
    fun impedanciaTransformador(potenciaVa: Double, tension: Double, ucc: Double): Double {
        positivo(potenciaVa, tension, ucc)
        return ucc * tension * tension / potenciaVa
    }

    /** Impedancia de la red aguas arriba a partir de su potencia de cortocircuito: Zr = U² / Scc. */
    fun impedanciaRed(sccVa: Double, tension: Double): Double {
        positivo(sccVa, tension)
        return tension * tension / sccVa
    }

    /** Icc trifásica en bornes de BT del transformador, con la red opcional: U / (√3 · (Zr + Zt)). */
    fun transformador(potenciaVa: Double, tension: Double, ucc: Double, sccRedVa: Double? = null): Double {
        val z = impedanciaTransformador(potenciaVa, tension, ucc) + (sccRedVa?.let { impedanciaRed(it, tension) } ?: 0.0)
        return tension / (sqrt(3.0) * z)
    }

    /**
     * Icc trifásica al final de una línea conociendo la del origen: la impedancia aguas arriba es
     * U / (√3·Icc0) y se le suma la del conductor (R y X de una fase, en Ω).
     */
    fun aguasAbajo(iccOrigen: Double, tension: Double, r: Double, x: Double): Double {
        positivo(iccOrigen, tension)
        if (r < 0 || x < 0) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        val z0 = tension / (sqrt(3.0) * iccOrigen)
        return tension / (sqrt(3.0) * hypot(z0 + r, x))
    }

    /**
     * Icc mínima fase-neutro (o fase-protección) al final de la línea: U0 / (Z0 + 2·Z línea), con Z0 la
     * impedancia de origen por fase. Para el bucle se suman ida y vuelta.
     */
    fun minima(iccOrigen: Double, tensionFaseNeutro: Double, r: Double, x: Double): Double {
        positivo(iccOrigen, tensionFaseNeutro)
        if (r < 0 || x < 0) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        val z0 = tensionFaseNeutro / iccOrigen
        return tensionFaseNeutro / hypot(z0 + 2 * r, 2 * x)
    }

    /** Largo máximo para que la Icc mínima al final del bucle alcance la corriente de disparo Ia. */
    fun largoMaximo(tensionFaseNeutro: Double, disparo: Double, rKm: Double, xKm: Double, iccOrigen: Double? = null): Double {
        positivo(tensionFaseNeutro, disparo, rKm)
        val zMax = tensionFaseNeutro / disparo
        val z0 = iccOrigen?.let { tensionFaseNeutro / it } ?: 0.0
        if (z0 >= zMax) return 0.0
        // |(z0 + 2rL) + j2xL| = zMax  →  4(r²+x²)L² + 4·z0·r·L + z0² − zMax² = 0
        val a = 4 * (rKm * rKm + xKm * xKm)
        val b = 4 * z0 * rKm
        val c = z0 * z0 - zMax * zMax
        return (-b + sqrt(b * b - 4 * a * c)) / (2 * a)
    }
}

/** Coordinación de la protección con el conductor: Ib ≤ In ≤ Iz. */
object Coordinacion {
    fun cumple(ib: Double, `in`: Double, iz: Double): Boolean {
        positivo(ib, `in`, iz)
        return ib <= `in` && `in` <= iz
    }
}
