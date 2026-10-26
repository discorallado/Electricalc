package cl.electricalc.datos.ric

import kotlin.math.roundToInt

/** Capacidad corregida de un conductor (RIC N°04, numeral 6.2.6: Ic = It × fn × ft). */
data class CapacidadCorregida(val mm2: Double, val awg: String?, val it: Int, val fn: Double, val ft: Double) {
    val ic: Double get() = it * fn * ft
}

object CapacidadRic {
    /** Ic de una sección; null si la tabla no la cubre (sección, método o temperatura fuera de rango). */
    fun corregida(temperaturaServicio: Int, metodo: String, mm2: Double, temperaturaAmbiente: Double, conductores: Int): CapacidadCorregida? {
        val fila = Ric.capacidad.bloque(temperaturaServicio)?.filas?.firstOrNull { it.mm2 == mm2 } ?: return null
        val it = fila.amperes[metodo] ?: return null
        val columna = Ric.ft.columna(temperaturaServicio, metodo) ?: return null
        val ft = Ric.ft.ft(columna.id, temperaturaAmbiente.roundToInt()) ?: return null
        return CapacidadCorregida(mm2, fila.awg, it, Ric.fn.fn(conductores), ft)
    }

    /** Menor sección de la tabla N°4.4 cuya Ic cubre [corriente] y no baja de [minimaMm2]. */
    fun seccionPara(
        corriente: Double, temperaturaServicio: Int, metodo: String, temperaturaAmbiente: Double, conductores: Int,
        minimaMm2: Double = 0.0,
    ): CapacidadCorregida? =
        Ric.capacidad.bloque(temperaturaServicio)?.filas.orEmpty()
            .filter { it.mm2 >= minimaMm2 - 1e-9 }
            .firstNotNullOfOrNull { f ->
                corregida(temperaturaServicio, metodo, f.mm2, temperaturaAmbiente, conductores)?.takeIf { it.ic >= corriente }
            }
}
