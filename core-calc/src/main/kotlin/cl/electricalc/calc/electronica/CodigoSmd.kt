package cl.electricalc.calc.electronica

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import kotlin.math.pow

/**
 * Código impreso en resistencias SMD, como lo interpreta la app de referencia:
 * puente (0, 00, 0R…), 3 o 4 cifras (la última es el exponente), punto decimal con R/K/M/L,
 * EIA-96 (dos cifras de la serie E96 y letra multiplicadora) y la variante con letra delante
 * (series E24 y E12, tolerancias de 2, 5 y 10 %).
 */
object CodigoSmd {
    enum class Tipo { PUENTE, CIFRAS, EIA96, LETRA_DELANTE }

    data class Lectura(val tipo: Tipo, val ohm: Double, val toleranciaPct: Double?)

    private val E96 = intArrayOf(
        100, 102, 105, 107, 110, 113, 115, 118, 121, 124, 127, 130, 133, 137, 140, 143, 147, 150, 154, 158, 162, 165,
        169, 174, 178, 182, 187, 191, 196, 200, 205, 210, 215, 221, 226, 232, 237, 243, 249, 255, 261, 267, 274, 280,
        287, 294, 301, 309, 316, 324, 332, 340, 348, 357, 365, 374, 383, 392, 402, 412, 422, 432, 442, 453, 464, 475,
        487, 499, 511, 523, 536, 549, 562, 576, 590, 604, 619, 634, 649, 665, 681, 698, 715, 732, 750, 768, 787, 806,
        825, 845, 866, 887, 909, 931, 953, 976,
    )

    /** Letra delante: 01–24 serie E24 (2 %), 25–48 E24 (5 %), 49–60 E12 (10 %). */
    private val E24_E24_E12 = intArrayOf(
        100, 110, 120, 130, 150, 160, 180, 200, 220, 240, 270, 300, 330, 360, 390, 430, 470, 510, 560, 620, 680, 750, 820, 910,
        100, 110, 120, 130, 150, 160, 180, 200, 220, 240, 270, 300, 330, 360, 390, 430, 470, 510, 560, 620, 680, 750, 820, 910,
        100, 120, 150, 180, 220, 270, 330, 390, 470, 560, 680, 820,
    )

    private val MULTIPLICADOR = mapOf(
        'Z' to 0.001, 'Y' to 0.01, 'X' to 0.1, 'S' to 0.1, 'A' to 1.0, 'B' to 10.0, 'H' to 10.0,
        'C' to 100.0, 'D' to 1000.0, 'E' to 10000.0, 'F' to 100000.0,
    )

    /** Letra final de tolerancia, en códigos de 4 o 5 caracteres. */
    val TOLERANCIA = mapOf('M' to 20.0, 'K' to 10.0, 'J' to 5.0, 'F' to 1.0, 'G' to 2.0, 'D' to 0.5, 'C' to 0.25, 'B' to 0.1)

    private val PUENTES = setOf("0", "00", "000", "0000", "0R0", "0R")

    private fun invalido(): Nothing = throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)

    /** [conLetraTolerancia]: el código termina en una letra de tolerancia (por ejemplo 4702F). */
    fun leer(codigo: String, conLetraTolerancia: Boolean = false): Lectura {
        var e = codigo.trim().uppercase()
        var tolLetra: Double? = null
        if (conLetraTolerancia) {
            if (e.length !in 4..5) invalido()
            tolLetra = TOLERANCIA[e.last()] ?: invalido()
            e = e.dropLast(1)
        }
        if (e in PUENTES) return Lectura(Tipo.PUENTE, 0.0, tolLetra)
        if (e.length == 3 && e.last() in MULTIPLICADOR) {
            val n = e.take(2).toIntOrNull()?.takeIf { it in 1..96 } ?: invalido()
            return Lectura(Tipo.EIA96, E96[n - 1] * MULTIPLICADOR.getValue(e.last()), tolLetra ?: 1.0)
        }
        if (e.length == 3 && e.first() in MULTIPLICADOR) {
            val n = e.drop(1).toIntOrNull()?.takeIf { it in 1..60 } ?: invalido()
            val tol = when (n) { in 1..24 -> 2.0; in 25..48 -> 5.0; else -> 10.0 }
            return Lectura(Tipo.LETRA_DELANTE, E24_E24_E12[n - 1] * MULTIPLICADOR.getValue(e.first()), tolLetra ?: tol)
        }
        if (e.length > 5 || e.length < 3) invalido()
        if (e.any { !it.isDigit() && it !in "RKML" }) invalido()
        val separador = e.firstOrNull { it in "RKML" }
        val ohm = if (separador != null) {
            if (e.count { it in "RKML" } > 1) invalido()
            val numero = e.replace(separador, '.').toDoubleOrNull() ?: invalido()
            numero * when (separador) { 'R' -> 1.0; 'K' -> 1e3; 'M' -> 1e6; else -> 1e-3 }
        } else if (e.startsWith("0")) {
            // Un cero inicial indica decimales: 047 = 0,47 Ω; 00x = 0,x Ω.
            ("0." + e.trimStart('0')).toDoubleOrNull() ?: invalido()
        } else {
            e.dropLast(1).toDouble() * 10.0.pow(e.last().digitToInt())
        }
        return Lectura(Tipo.CIFRAS, ohm, tolLetra ?: if (e.length == 4) 1.0 else null)
    }
}
