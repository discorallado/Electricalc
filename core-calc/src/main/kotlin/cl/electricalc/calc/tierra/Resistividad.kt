package cl.electricalc.calc.tierra

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Resistividad aparente del terreno con cuatro electrodos en línea: corriente por los extremos (A, B)
 * y tensión entre los interiores (M, N). Con R = ΔV/I medido por el telurómetro:
 *
 *   ρ = 2π·R / (1/AM − 1/BM − 1/AN + 1/BN)
 *
 * Wenner y Schlumberger son dos disposiciones de esa misma expresión.
 */
object Resistividad {

    private fun positivo(vararg valores: Double) {
        if (valores.any { !(it > 0.0) || it.isInfinite() }) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
    }

    /** Wenner: cuatro electrodos a la misma separación [a] (m), clavados a poca profundidad frente a [a]. */
    fun wenner(a: Double, resistencia: Double): Double {
        positivo(a, resistencia)
        return 2 * PI * a * resistencia
    }

    /**
     * Wenner con la profundidad [b] (m) de los electrodos: corrige el caso en que b no es pequeña frente
     * a [a]. Con b → 0 tiende a [wenner].
     */
    fun wennerProfundo(a: Double, b: Double, resistencia: Double): Double {
        positivo(a, b, resistencia)
        val denominador = 1 + 2 * a / sqrt(a * a + 4 * b * b) - a / sqrt(a * a + b * b)
        return 4 * PI * a * resistencia / denominador
    }

    /**
     * Schlumberger: [semiCorriente] = AB/2 (distancia del centro a cada electrodo de corriente) y
     * [semiPotencial] = MN/2, con MN/2 < AB/2.
     */
    fun schlumberger(semiCorriente: Double, semiPotencial: Double, resistencia: Double): Double {
        positivo(semiCorriente, semiPotencial, resistencia)
        if (semiPotencial >= semiCorriente) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        val l2 = semiCorriente * semiCorriente
        val m2 = semiPotencial * semiPotencial
        return PI * resistencia * (l2 - m2) / (2 * semiPotencial)
    }

    /** Ala de medición (distancia del centro al electrodo de corriente más lejano) de una disposición Wenner. */
    fun alaWenner(a: Double): Double = 1.5 * a
}
