package cl.electricalc.calc.caida

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.comun.requiereCosPhi
import cl.electricalc.calc.comun.requierePositivo
import cl.electricalc.calc.conductores.Cable
import kotlin.math.sqrt

/**
 * Caída de tensión en una línea.
 *
 *   ΔV = f · I · L · (R·cosφ + X·senφ) · (n + 1) / 2 / paralelos
 *
 * con f = 2 en CC, monofásica y bifásica (ida y vuelta) y √3 en trifásica; L en km; R y X en Ω/km.
 * Con n cargas iguales repartidas uniformemente a lo largo de la línea, el factor (n + 1)/2 reemplaza
 * a la carga concentrada al final (n = 1). En CC no hay reactancia y cosφ = 1.
 */
object CaidaTension {

    data class Resultado(
        /** Caída en voltios (acotada a la tensión de alimentación). */
        val caidaV: Double,
        /** Caída en % de la tensión de alimentación. */
        val porcentaje: Double,
        /** Tensión al final de la línea. */
        val tensionFinal: Double,
    )

    fun factorLinea(sistema: SistemaElectrico): Double =
        if (sistema == SistemaElectrico.TRIFASICA) sqrt(3.0) else 2.0

    /** Ω/km "efectivos" por conductor: R·cosφ + X·senφ. */
    fun impedanciaEfectiva(sistema: SistemaElectrico, cable: Cable, cosPhi: Double): Double {
        if (!sistema.esAlterna) return cable.resistenciaOhmKm(alterna = false)
        val sen = sqrt(1.0 - cosPhi * cosPhi)
        return cable.resistenciaOhmKm(alterna = true) * cosPhi + cable.reactanciaOhmKm() * sen
    }

    fun calcular(
        sistema: SistemaElectrico,
        tension: Double,
        corriente: Double,
        cosPhi: Double,
        cable: Cable,
        cargasDistribuidas: Int = 1,
    ): Resultado {
        requierePositivo(tension, MotivoError.TENSION_NO_VALIDA)
        requierePositivo(corriente, MotivoError.CORRIENTE_NO_VALIDA)
        requierePositivo(cable.longitudM, MotivoError.VALOR_FUERA_DE_RANGO)
        if (cargasDistribuidas < 1) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        val cos = if (sistema.esAlterna) requiereCosPhi(cosPhi) else 1.0
        val caida = factorLinea(sistema) * corriente * (cable.longitudM / 1000.0) *
            impedanciaEfectiva(sistema, cable, cos) * ((cargasDistribuidas + 1) / 2.0) / cable.paralelos
        val acotada = minOf(caida, tension)
        return Resultado(acotada, acotada * 100.0 / tension, tension - acotada)
    }

    /** Longitud máxima (m) para no superar [caidaMaxV] con carga al final de la línea. */
    fun longitudMaxima(
        sistema: SistemaElectrico,
        tension: Double,
        corriente: Double,
        cosPhi: Double,
        cable: Cable,
        caidaMaxV: Double,
    ): Double {
        requierePositivo(tension, MotivoError.TENSION_NO_VALIDA)
        requierePositivo(corriente, MotivoError.CORRIENTE_NO_VALIDA)
        if (!(caidaMaxV > 0.0 && caidaMaxV < tension)) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        val cos = if (sistema.esAlterna) requiereCosPhi(cosPhi) else 1.0
        val porKm = factorLinea(sistema) * corriente * impedanciaEfectiva(sistema, cable, cos)
        return caidaMaxV / porKm * 1000.0 * cable.paralelos
    }
}
