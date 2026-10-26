package cl.electricalc.datos.ric

/** Semáforo de una verificación: cumple (verde), no cumple (rojo) o revisar (ámbar, falta un dato). */
enum class EstadoVerificacion { CUMPLE, NO_CUMPLE, REVISAR }

/** Qué se verificó; la app lo usa para elegir el texto. */
enum class TipoVerificacion { CAIDA_TRAMO, CAIDA_TOTAL, SECCION_MINIMA, TENSION_CONTACTO, AISLAMIENTO }

/**
 * Resultado de comparar un valor calculado con un límite del RIC. [limite] y [calculado] van en la
 * unidad del límite (% o mm²); [fuente] da el pliego, el numeral y la tabla para citarlo.
 */
data class Verificacion(
    val tipo: TipoVerificacion,
    val estado: EstadoVerificacion,
    val calculado: Double?,
    val limite: Double?,
    val fuente: FuenteRic,
    /** Uso de la línea, en las verificaciones de sección. */
    val uso: SeccionMinima? = null,
)

object VerificacionRic {
    /**
     * Caída de un tramo (alimentador, subalimentador o conductor) frente al límite por tramo, y la
     * misma caída frente al límite total: si un tramo solo ya lo supera, no hay margen para el resto.
     */
    fun caida(porcentaje: Double): List<Verificacion> {
        val c = Ric.caidas
        return listOf(
            Verificacion(TipoVerificacion.CAIDA_TRAMO, estado(porcentaje <= c.porTramo), porcentaje, c.porTramo, c),
            Verificacion(TipoVerificacion.CAIDA_TOTAL, estado(porcentaje <= c.total), porcentaje, c.total, c),
        )
    }

    /** Sección del conductor frente a la mínima para su uso; sin uso indicado queda en "revisar". */
    fun seccion(mm2: Double, uso: String?): Verificacion {
        val tabla = Ric.seccionesMinimas
        val minima = uso?.let(tabla::minima)
            ?: return Verificacion(TipoVerificacion.SECCION_MINIMA, EstadoVerificacion.REVISAR, mm2, null, tabla)
        // Las secciones AWG se comparan por su área; una tolerancia mínima evita falsos negativos por redondeo.
        return Verificacion(
            TipoVerificacion.SECCION_MINIMA, estado(mm2 >= minima.mm2 - 0.005), mm2, minima.mm2, tabla, minima,
        )
    }

    /** Esquema TT: RA × Ia ≤ UL (RIC N°05, numeral 8.7.7.3). */
    fun tt(resistenciaOhm: Double, sensibilidadA: Double, humedo: Boolean, continua: Boolean = false): Verificacion {
        val c = Ric.contactos
        val ul = c.tensionLimite.valor(humedo, continua)
        val contacto = resistenciaOhm * sensibilidadA
        return Verificacion(TipoVerificacion.TENSION_CONTACTO, estado(contacto <= ul), contacto, ul, c)
    }

    /** Resistencia de aislamiento medida frente al mínimo de la tabla N°19.1 del RIC N°19. */
    fun aislamiento(medidaMohm: Double, circuito: String): Verificacion {
        val t = Ric.puestaEnServicio
        val minimo = t.aislamiento.first { it.circuito == circuito }.minimoMohm
        return Verificacion(TipoVerificacion.AISLAMIENTO, estado(medidaMohm >= minimo), medidaMohm, minimo, t)
    }

    private fun estado(cumple: Boolean) = if (cumple) EstadoVerificacion.CUMPLE else EstadoVerificacion.NO_CUMPLE
}
