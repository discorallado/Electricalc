package cl.electricalc.calc.electronica

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Código de colores de resistencias: cifras (negro 0 … blanco 9) y multiplicador como potencia de 10,
 * de −2 (plata) a 9 (blanco).
 */
object CodigoColores {
    const val EXPONENTE_MIN = -2
    const val EXPONENTE_MAX = 9

    fun valor(cifras: List<Int>, exponente: Int): Double {
        if (cifras.isEmpty() || cifras.any { it !in 0..9 } || exponente !in EXPONENTE_MIN..EXPONENTE_MAX) {
            throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        }
        return cifras.fold(0L) { n, c -> n * 10 + c } * 10.0.pow(exponente)
    }

    /**
     * Cifras y exponente para representar [ohm] con [n] cifras significativas (2 en 4 bandas, 3 en 5 o 6).
     * Falla si el valor no cabe en el rango de multiplicadores.
     */
    fun bandas(ohm: Double, n: Int): Pair<List<Int>, Int> {
        if (!(ohm > 0) || ohm.isInfinite() || n !in 2..3) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        var exponente = floor(log10(ohm)).toInt() - (n - 1)
        var mantisa = (ohm / 10.0.pow(exponente)).roundToLong()
        if (mantisa >= 10.0.pow(n).toLong()) { mantisa /= 10; exponente++ }
        if (exponente !in EXPONENTE_MIN..EXPONENTE_MAX) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return mantisa.toString().padStart(n, '0').map { it - '0' } to exponente
    }
}
