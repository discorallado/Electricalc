package cl.electricalc.calc.comun

/** Motivo por el que un cálculo no se puede completar. La UI lo traduce a un mensaje. */
enum class MotivoError {
    DATOS_INSUFICIENTES,
    NO_APLICA_EN_CONTINUA,
    TENSION_NO_VALIDA,
    CORRIENTE_NO_VALIDA,
    RESISTENCIA_NO_VALIDA,
    IMPEDANCIA_NO_VALIDA,
    POTENCIA_NO_VALIDA,
    COS_PHI_NO_VALIDO,
    VALOR_FUERA_DE_RANGO,
}

class ErrorCalculo(val motivo: MotivoError, mensaje: String? = null) :
    IllegalArgumentException(mensaje ?: motivo.name)

internal fun requierePositivo(valor: Double, motivo: MotivoError): Double {
    if (!(valor > 0.0) || valor.isInfinite()) throw ErrorCalculo(motivo)
    return valor
}

internal fun requiereCosPhi(valor: Double): Double {
    if (!(valor > 0.0 && valor <= 1.0)) throw ErrorCalculo(MotivoError.COS_PHI_NO_VALIDO)
    return valor
}

/** Igual que [requiereCosPhi], pero absorbe el redondeo de un cosφ calculado que roza 1. */
internal fun cosPhiCalculado(valor: Double): Double =
    requiereCosPhi(if (valor > 1.0 && valor <= 1.0 + 1e-9) 1.0 else valor)
