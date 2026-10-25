package cl.electricalc.app.ui.calculadoras

import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.ohm.Magnitud
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

/** Unidades de los campos. Los símbolos son universales y no se traducen. */
object Unidades {
    val V = Unidad("V")
    val mV = Unidad("mV") { it / 1e3 }
    val kV = Unidad("kV") { it * 1e3 }

    val A = Unidad("A", Magnitud.CORRIENTE)
    val mA = Unidad("mA", Magnitud.CORRIENTE) { it / 1e3 }
    val kA = Unidad("kA", Magnitud.CORRIENTE) { it * 1e3 }

    val ohm = Unidad("Ω")
    val mOhm = Unidad("mΩ") { it / 1e3 }
    val kOhm = Unidad("kΩ") { it * 1e3 }
    val megaOhm = Unidad("MΩ") { it * 1e6 }

    val W = Unidad("W", Magnitud.POTENCIA_ACTIVA)
    val kW = Unidad("kW", Magnitud.POTENCIA_ACTIVA) { it * 1e3 }
    val MW = Unidad("MW", Magnitud.POTENCIA_ACTIVA) { it * 1e6 }
    val HP = Unidad("HP", Magnitud.POTENCIA_ACTIVA) { it * 745.699872 }
    val CV = Unidad("CV", Magnitud.POTENCIA_ACTIVA) { it * 735.49875 }

    val VA = Unidad("VA", Magnitud.POTENCIA_APARENTE)
    val kVA = Unidad("kVA", Magnitud.POTENCIA_APARENTE) { it * 1e3 }
    val MVA = Unidad("MVA", Magnitud.POTENCIA_APARENTE) { it * 1e6 }

    val varU = Unidad("var", Magnitud.POTENCIA_REACTIVA)
    val kvar = Unidad("kvar", Magnitud.POTENCIA_REACTIVA) { it * 1e3 }
    val Mvar = Unidad("Mvar", Magnitud.POTENCIA_REACTIVA) { it * 1e6 }

    val m = Unidad("m")
    val ft = Unidad("ft") { it * 0.3048 }
    val yd = Unidad("yd") { it * 0.9144 }

    val celsius = Unidad("°C")
    val fahrenheit = Unidad("°F") { (it - 32.0) * 5.0 / 9.0 }

    /** Formas de indicar el desfase; todas se llevan a cosφ. Fuera de rango dan NaN y el campo se rechaza. */
    val cosPhi = Unidad("cos φ")
    val senPhi = Unidad("sen φ") { if (it in 0.0..1.0) sqrt(1 - it * it) else Double.NaN }
    val tanPhi = Unidad("tan φ") { if (it >= 0) 1 / sqrt(1 + it * it) else Double.NaN }
    val phiGrados = Unidad("φ (°)") { if (it in 0.0..90.0) cos(it * PI / 180) else Double.NaN }
    val phiRad = Unidad("φ (rad)") { if (it in 0.0..PI / 2) cos(it) else Double.NaN }

    val tension = listOf(mV, V, kV)
    val corriente = listOf(mA, A, kA)
    val resistencia = listOf(mOhm, ohm, kOhm, megaOhm)
    val potenciaActiva = listOf(W, kW, MW, HP, CV)
    val potenciaAparente = listOf(VA, kVA, MVA)
    val potenciaReactiva = listOf(varU, kvar, Mvar)
    val fase = listOf(cosPhi, senPhi, tanPhi, phiGrados, phiRad)
    val longitud = listOf(m, ft, yd)
    val temperatura = listOf(celsius, fahrenheit)

    /** Potencia "genérica": la unidad elegida dice si es activa, aparente o reactiva. */
    fun potencia(sistema: SistemaElectrico) =
        if (sistema.esAlterna) potenciaActiva + potenciaAparente + potenciaReactiva else potenciaActiva

    /** Carga de una línea: corriente o cualquier potencia. */
    fun carga(sistema: SistemaElectrico) =
        listOf(W, kW, A, HP, CV, VA, kVA, MVA) + if (sistema.esAlterna) potenciaReactiva else emptyList()
}

object Formato {
    private fun formato(decimales: Int) = DecimalFormat(
        "#,##0" + if (decimales > 0) "." + "#".repeat(decimales) else "",
        DecimalFormatSymbols.getInstance(Locale.getDefault()),
    )

    fun numero(valor: Double, decimales: Int = 3): String = formato(decimales).format(valor)

    /** Con decimales fijos (porcentajes): 0,998 → "1,00". */
    fun fijo(valor: Double, decimales: Int = 2): String =
        DecimalFormat("#,##0." + "0".repeat(decimales), DecimalFormatSymbols.getInstance(Locale.getDefault())).format(valor)

    private val prefijos = listOf(1e9 to "G", 1e6 to "M", 1e3 to "k", 1.0 to "", 1e-3 to "m", 1e-6 to "µ")

    /** Valor con el prefijo SI que lo deja entre 1 y 1000: 12 500 W → "12,5 kW". */
    fun conPrefijo(valor: Double, simbolo: String, decimales: Int = 3): String {
        val a = abs(valor)
        val (factor, prefijo) = if (a == 0.0) 1.0 to "" else prefijos.firstOrNull { a >= it.first } ?: prefijos.last()
        return "${numero(valor / factor, decimales)} $prefijo$simbolo"
    }

    /** Interpreta coma o punto decimal. Null si el texto está vacío o no es un número. */
    fun leer(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()
}
