package cl.electricalc.app.ui.catalogo

import java.text.Normalizer

/**
 * Buscador global sobre títulos (español e inglés), sinónimos, categoría y número de pliego.
 * No distingue mayúsculas ni tildes. "RIC 4", "ric n°04" o "ric4" llevan al pliego y a sus
 * herramientas; el resto del texto filtra dentro de ellas.
 */
class Buscador(
    private val herramientas: List<Herramienta>,
    private val pliegos: List<Pliego>,
    nombresCategoria: Map<String, String> = emptyMap(),
) {
    data class Resultados(val pliegos: List<Pliego>, val herramientas: List<Herramienta>) {
        val vacio: Boolean get() = pliegos.isEmpty() && herramientas.isEmpty()
    }

    private class Indice(val herramienta: Herramienta, val titulo: List<String>, val resto: List<String>)

    private val indice = herramientas.map { h ->
        Indice(
            h,
            palabras(h.tituloEs + " " + h.tituloEn.orEmpty()),
            palabras((h.sinonimos + nombresCategoria[h.categoria].orEmpty() + h.descripcion.orEmpty()).joinToString(" ")),
        )
    }
    private val titulosPliego = pliegos.associateWith { palabras(it.titulo) }

    fun buscar(consulta: String): Resultados {
        var texto = normalizar(consulta)
        if (texto.isBlank()) return Resultados(emptyList(), emptyList())

        val numero = RIC.find(texto)?.let { m ->
            texto = texto.removeRange(m.range)
            m.groupValues[1].toInt()
        }?.takeIf { n -> pliegos.any { it.numero == n } }
        val tokens = texto.split(' ').filter { it.isNotBlank() }

        val candidatos = if (numero != null) indice.filter { numero in it.herramienta.ric } else indice
        val encontradas = candidatos
            .mapNotNull { i -> puntaje(i, tokens)?.let { i.herramienta to it } }
            // Las fichas del formulario repiten el tema de su calculadora: van después.
            .sortedWith(compareByDescending<Pair<Herramienta, Int>> { it.second }.thenBy { it.first.esFicha })
            .map { it.first }

        val pliegosEncontrados = when {
            numero != null -> pliegos.filter { it.numero == numero }
            tokens.isEmpty() -> emptyList()
            else -> pliegos.filter { p -> tokens.all { t -> titulosPliego.getValue(p).any { it.startsWith(t) } } }
        }
        return Resultados(pliegosEncontrados, encontradas)
    }

    /** Null si algún término no aparece. Pesa más el título que los sinónimos. */
    private fun puntaje(i: Indice, tokens: List<String>): Int? {
        if (tokens.isEmpty()) return 0
        var total = 0
        for (t in tokens) {
            total += when {
                i.titulo.any { it == t } -> 3
                i.titulo.any { it.startsWith(t) } -> 2
                i.resto.any { it.startsWith(t) } -> 1
                else -> return null
            }
        }
        return total
    }

    companion object {
        private val RIC = Regex("""\bric\s*(?:n\s*)?0*(\d{1,2})\b""")

        /** Minúsculas, sin tildes y sin signos: "RIC N°04 – Caída" → "ric n 04 caida". */
        fun normalizar(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[ºª]"), " ")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

        private fun palabras(s: String) = normalizar(s).split(' ').filter { it.isNotBlank() }
    }
}
