package cl.electricalc.calc.comun

import kotlin.math.sqrt

/**
 * Tipo de sistema de alimentación.
 *
 * [factor] multiplica la potencia: en trifásica la tensión es la de línea y S = √3·V·I.
 * La bifásica se trata como monofásica entre dos fases (S = V·I).
 */
enum class SistemaElectrico(val factor: Double, val esAlterna: Boolean) {
    CONTINUA(1.0, false),
    MONOFASICA(1.0, true),
    BIFASICA(1.0, true),
    TRIFASICA(sqrt(3.0), true),
}
