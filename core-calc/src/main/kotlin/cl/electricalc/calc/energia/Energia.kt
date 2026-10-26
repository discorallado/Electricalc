package cl.electricalc.calc.energia

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import kotlin.math.ceil
import kotlin.math.sqrt

private fun positivo(vararg v: Double) {
    if (v.any { !(it > 0.0) || it.isInfinite() }) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
}

/** Consumo de energía y su costo. */
object Consumo {
    /** kWh en el período: potencia (W) × horas por día × días. */
    fun kwh(potenciaW: Double, horasDia: Double, dias: Double): Double {
        positivo(potenciaW, horasDia, dias)
        if (horasDia > 24) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return potenciaW * horasDia * dias / 1000
    }
}

/** Iluminación de interiores por el método de los lúmenes. */
object Lumenes {
    /** Índice del local K = L·A / (h·(L + A)), con h la altura entre luminaria y plano de trabajo. */
    fun indiceLocal(largo: Double, ancho: Double, altura: Double): Double {
        positivo(largo, ancho, altura)
        return largo * ancho / (altura * (largo + ancho))
    }

    /** Flujo total necesario Φ = E·S / (CU·FM). */
    fun flujoTotal(iluminanciaLux: Double, superficieM2: Double, utilizacion: Double, mantenimiento: Double): Double {
        positivo(iluminanciaLux, superficieM2, utilizacion, mantenimiento)
        if (utilizacion > 1 || mantenimiento > 1) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return iluminanciaLux * superficieM2 / (utilizacion * mantenimiento)
    }

    /** Luminarias necesarias (redondeo hacia arriba). */
    fun luminarias(flujoTotal: Double, flujoLuminaria: Double): Int {
        positivo(flujoTotal, flujoLuminaria)
        return ceil(flujoTotal / flujoLuminaria - 1e-9).toInt()
    }

    /** Iluminancia media que se obtiene con [n] luminarias. */
    fun iluminanciaObtenida(n: Int, flujoLuminaria: Double, superficieM2: Double, utilizacion: Double, mantenimiento: Double): Double =
        n * flujoLuminaria * utilizacion * mantenimiento / superficieM2
}

/** Compensación de potencia reactiva con condensadores. */
object Compensacion {
    private fun tan(cos: Double) = sqrt(1 - cos * cos) / cos

    /** Potencia reactiva (var) a instalar para pasar de cosφ1 a cosφ2 con potencia activa P (W). */
    fun reactiva(potenciaW: Double, cosActual: Double, cosObjetivo: Double): Double {
        positivo(potenciaW)
        if (cosActual !in 0.01..1.0 || cosObjetivo !in 0.01..1.0 || cosObjetivo < cosActual) {
            throw ErrorCalculo(MotivoError.COS_PHI_NO_VALIDO)
        }
        return potenciaW * (tan(cosActual) - tan(cosObjetivo))
    }

    /** Capacidad (F) por fase de un banco en estrella para Q (var) a tensión de línea U y frecuencia f. */
    fun capacidadEstrella(reactivaVar: Double, tensionLinea: Double, frecuencia: Double = 50.0): Double {
        positivo(reactivaVar, tensionLinea, frecuencia)
        return reactivaVar / (2 * Math.PI * frecuencia * tensionLinea * tensionLinea)
    }
}

/** Respaldo con UPS o grupo electrógeno. */
object Respaldo {
    /** Potencia aparente (VA) del equipo con un margen de reserva (p. ej. 0,25 = 25 %). */
    fun potenciaAparente(potenciaW: Double, cosPhi: Double, reserva: Double): Double {
        positivo(potenciaW, cosPhi)
        if (cosPhi > 1 || reserva < 0) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return potenciaW / cosPhi * (1 + reserva)
    }

    /** Capacidad de baterías (Ah) para [autonomiaH] horas: P·t / (V·η·profundidad de descarga). */
    fun capacidadBaterias(potenciaW: Double, autonomiaH: Double, tensionBanco: Double, rendimiento: Double, descarga: Double): Double {
        positivo(potenciaW, autonomiaH, tensionBanco, rendimiento, descarga)
        if (rendimiento > 1 || descarga > 1) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return potenciaW * autonomiaH / (tensionBanco * rendimiento * descarga)
    }

    /** Autonomía en horas de un banco de [capacidadAh] a [tensionBanco]: C·U·η·DoD / P. */
    fun autonomia(capacidadAh: Double, tensionBanco: Double, potenciaW: Double, rendimiento: Double, descarga: Double): Double {
        positivo(capacidadAh, tensionBanco, potenciaW, rendimiento, descarga)
        if (rendimiento > 1 || descarga > 1) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return capacidadAh * tensionBanco * rendimiento * descarga / potenciaW
    }
}
