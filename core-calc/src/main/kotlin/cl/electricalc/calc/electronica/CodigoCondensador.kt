package cl.electricalc.calc.electronica

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError

/**
 * Código impreso en condensadores, como lo interpreta la app de referencia: 1 o 2 cifras en pF,
 * 3 cifras (dos cifras y multiplicador; 8 = ×0,01 y 9 = ×0,1), ".1" en µF, letras R/P/N/U como coma
 * (4N7 = 4,7 nF), letra final de tolerancia y prefijo de tensión de 2 caracteres en códigos de 5 o más.
 */
object CodigoCondensador {
    data class Lectura(val pf: Double, val tolerancia: String?, val tensionV: Double?)

    private val MULTIPLICADOR = doubleArrayOf(1.0, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 1e6, 1e7, 0.01, 0.1)

    val TENSION = mapOf(
        "0J" to 6.3, "1A" to 10.0, "1C" to 16.0, "1E" to 25.0, "1V" to 35.0, "1H" to 50.0, "1J" to 63.0,
        "2A" to 100.0, "2D" to 200.0, "2E" to 250.0, "2G" to 400.0, "2J" to 630.0,
    )

    private val TOLERANCIA = mapOf(
        'B' to "±0,1 %", 'C' to "±0,25 %", 'D' to "±0,5 %", 'F' to "±1 %", 'G' to "±2 %", 'H' to "±3 %",
        'J' to "±5 %", 'K' to "±10 %", 'M' to "±20 %", 'P' to "−0 / +100 %", 'Z' to "−20 % / +80 %",
    )

    /** Hasta 10 pF algunas letras indican la tolerancia en pF. */
    private val TOLERANCIA_PF = mapOf('B' to "±0,1 pF", 'C' to "±0,25 pF", 'D' to "±0,5 pF", 'F' to "±1 pF", 'G' to "±2 pF")

    private val CON_LETRA = Regex("[0-9]*[RPNU][0-9]*")

    private fun invalido(): Nothing = throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)

    private fun conComa(s: String) = s.length in 2..5 && CON_LETRA.matches(s)

    private fun tolerancia(s: String, pf: Double?): String? {
        if (s.length !in 2..6 || conComa(s)) return null
        val letra = s.last()
        if (pf != null && pf <= 10) TOLERANCIA_PF[letra]?.let { return it }
        return TOLERANCIA[letra]
    }

    private fun valor(s: String): Double {
        if (Regex("\\.[0-9]+").matches(s)) return ("0$s").toDoubleOrNull()?.times(1e6) ?: invalido()
        val directo = if (s.length >= 3) s.length == 3 && s.endsWith(".") && s.toDoubleOrNull() != null else s.toIntOrNull() != null
        if (directo) return s.toDoubleOrNull() ?: invalido()
        if (s.length == 3 && s.toIntOrNull() != null) return s.take(2).toInt() * MULTIPLICADOR[s[2].digitToInt()]
        if (!conComa(s)) {
            if (tolerancia(s, null) != null) return valor(s.dropLast(1))
            if (s.length >= 5 && s.take(2) in TENSION) return valor(s.drop(2))
            invalido()
        }
        val numero = s.replace('R', '.').replace('P', '.').replace('N', '.').replace('U', '.').toDoubleOrNull() ?: invalido()
        return numero * when {
            'R' in s || 'P' in s -> 1.0
            'N' in s -> 1000.0
            else -> 1e6
        }
    }

    fun leer(codigo: String): Lectura {
        val s = codigo.trim().uppercase().replace('Μ', 'U').replace('µ', 'U')
        if ('-' in s || '+' in s || s.isEmpty()) invalido()
        val pf = valor(s)
        return Lectura(pf, tolerancia(s, pf), if (s.length >= 5) TENSION[s.take(2)] else null)
    }
}
