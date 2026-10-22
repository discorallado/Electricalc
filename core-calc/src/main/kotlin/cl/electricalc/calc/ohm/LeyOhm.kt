package cl.electricalc.calc.ohm

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.comun.cosPhiCalculado
import cl.electricalc.calc.comun.requiereCosPhi
import cl.electricalc.calc.comun.requierePositivo
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/** Magnitudes eléctricas de un circuito. */
enum class Magnitud(val motivoInvalido: MotivoError) {
    TENSION(MotivoError.TENSION_NO_VALIDA),
    CORRIENTE(MotivoError.CORRIENTE_NO_VALIDA),
    RESISTENCIA(MotivoError.RESISTENCIA_NO_VALIDA),
    IMPEDANCIA(MotivoError.IMPEDANCIA_NO_VALIDA),
    POTENCIA_ACTIVA(MotivoError.POTENCIA_NO_VALIDA),
    POTENCIA_REACTIVA(MotivoError.POTENCIA_NO_VALIDA),
    POTENCIA_APARENTE(MotivoError.POTENCIA_NO_VALIDA),
}

/**
 * Ley de Ohm y potencias en CC, monofásica, bifásica y trifásica.
 *
 * Convenciones (trifásica con tensión de línea V, k = √3; resto k = 1):
 *  - Z = V / I,  R = Z·cosφ
 *  - S = k·V·I,  P = S·cosφ,  Q = S·senφ
 *  - En continua cosφ = 1, Z = R y no existe Q.
 *
 * Cada magnitud cumple ln(m) = α·ln(V) + β·ln(I) + desplazamiento(k, φ), así que el problema es
 * lineal en (ln V, ln I): con dos datos independientes se conoce todo, y con uno solo se pueden
 * obtener las magnitudes proporcionales a él.
 */
object LeyOhm {

    /** Datos de entrada: magnitudes conocidas y cosφ (se ignora en continua o si los datos lo determinan). */
    data class Datos(
        val sistema: SistemaElectrico,
        val conocidas: Map<Magnitud, Double>,
        val cosPhi: Double = 1.0,
    )

    fun calcular(datos: Datos, salida: Magnitud): Double {
        val estado = preparar(datos)
        return estado.resolver(salida)
    }

    fun calcularCosPhi(datos: Datos): Double {
        if (!datos.sistema.esAlterna) {
            validarContinua(datos.conocidas.keys)
            return 1.0
        }
        validarEntradas(datos.conocidas)
        cosPhiDeterminado(datos.conocidas)?.let { return it }
        // Con V, I (o Z, S) conocidos, el cosφ sale de R, P o Q.
        val sinFase = datos.conocidas.filterKeys { !it.dependeDeFase() }
        val base = Estado(datos.sistema, 1.0, sinFase)
        for ((m, valor) in datos.conocidas) {
            if (!m.dependeDeFase()) continue
            val sinCos = runCatching { base.resolverSinFase(m) }.getOrNull() ?: continue
            val razon = valor / sinCos
            return when (m) {
                Magnitud.POTENCIA_REACTIVA -> {
                    if (razon >= 1.0) throw ErrorCalculo(MotivoError.COS_PHI_NO_VALIDO)
                    cosPhiCalculado(sqrt(1.0 - razon * razon))
                }
                else -> cosPhiCalculado(razon)
            }
        }
        throw ErrorCalculo(MotivoError.DATOS_INSUFICIENTES)
    }

    private fun preparar(datos: Datos): Estado {
        validarEntradas(datos.conocidas)
        if (!datos.sistema.esAlterna) {
            validarContinua(datos.conocidas.keys)
            return Estado(datos.sistema, 1.0, datos.conocidas)
        }
        val cos = cosPhiDeterminado(datos.conocidas) ?: requiereCosPhi(datos.cosPhi)
        return Estado(datos.sistema, cos, datos.conocidas)
    }

    private fun validarEntradas(conocidas: Map<Magnitud, Double>) {
        for ((m, v) in conocidas) requierePositivo(v, m.motivoInvalido)
    }

    private fun validarContinua(magnitudes: Collection<Magnitud>) {
        if (Magnitud.POTENCIA_REACTIVA in magnitudes) {
            throw ErrorCalculo(MotivoError.NO_APLICA_EN_CONTINUA)
        }
    }

    /** cosφ que se desprende de los datos (dos potencias, o R y Z), o null si no se desprende. */
    private fun cosPhiDeterminado(c: Map<Magnitud, Double>): Double? {
        val p = c[Magnitud.POTENCIA_ACTIVA]
        val q = c[Magnitud.POTENCIA_REACTIVA]
        val s = c[Magnitud.POTENCIA_APARENTE]
        val r = c[Magnitud.RESISTENCIA]
        val z = c[Magnitud.IMPEDANCIA]
        val cos = when {
            p != null && s != null -> p / s
            q != null && s != null -> if (q >= s) 0.0 else sqrt(1.0 - (q / s) * (q / s))
            p != null && q != null -> p / sqrt(p * p + q * q)
            r != null && z != null -> r / z
            else -> return null
        }
        if (!(cos > 0.0 && cos <= 1.0 + 1e-9)) throw ErrorCalculo(MotivoError.POTENCIA_NO_VALIDA)
        return cosPhiCalculado(cos)
    }

    private fun Magnitud.dependeDeFase() =
        this == Magnitud.RESISTENCIA || this == Magnitud.POTENCIA_ACTIVA || this == Magnitud.POTENCIA_REACTIVA

    /** Coeficientes (α, β) de ln V y ln I para cada magnitud. */
    private fun Magnitud.coeficientes(): Pair<Int, Int> = when (this) {
        Magnitud.TENSION -> 1 to 0
        Magnitud.CORRIENTE -> 0 to 1
        Magnitud.RESISTENCIA, Magnitud.IMPEDANCIA -> 1 to -1
        Magnitud.POTENCIA_ACTIVA, Magnitud.POTENCIA_REACTIVA, Magnitud.POTENCIA_APARENTE -> 1 to 1
    }

    private class Estado(
        val sistema: SistemaElectrico,
        val cosPhi: Double,
        val conocidas: Map<Magnitud, Double>,
    ) {
        private val senPhi = sqrt(1.0 - cosPhi * cosPhi)

        /** ln(m) − α·ln V − β·ln I, es decir, la parte que no depende de V ni de I. */
        fun desplazamiento(m: Magnitud, conFase: Boolean = true): Double {
            val lnK = ln(sistema.factor)
            return when (m) {
                Magnitud.TENSION, Magnitud.CORRIENTE, Magnitud.IMPEDANCIA -> 0.0
                Magnitud.RESISTENCIA -> if (conFase) ln(cosPhi) else 0.0
                Magnitud.POTENCIA_APARENTE -> lnK
                Magnitud.POTENCIA_ACTIVA -> lnK + if (conFase) ln(cosPhi) else 0.0
                Magnitud.POTENCIA_REACTIVA -> lnK + if (conFase) ln(senPhi) else 0.0
            }
        }

        fun resolver(salida: Magnitud): Double {
            if (!sistema.esAlterna) validarContinua(listOf(salida))
            if (salida == Magnitud.POTENCIA_REACTIVA && senPhi == 0.0) {
                throw ErrorCalculo(MotivoError.POTENCIA_NO_VALIDA)
            }
            val ln = lnDe(salida, conFase = true)
            return requierePositivo(exp(ln), salida.motivoInvalido)
        }

        /** Valor de m suponiendo cosφ = 1 en su desplazamiento (para despejar el cosφ). */
        fun resolverSinFase(m: Magnitud): Double = exp(lnDe(m, conFase = false))

        private fun lnDe(salida: Magnitud, conFase: Boolean): Double {
            val filas = conocidas.map { (m, v) ->
                val (a, b) = m.coeficientes()
                Triple(a.toDouble(), b.toDouble(), ln(v) - desplazamiento(m))
            }
            val (wa, wb) = salida.coeficientes()
            val off = desplazamiento(salida, conFase)
            // Dos filas independientes: se despejan ln V y ln I.
            for (i in filas.indices) for (j in i + 1 until filas.size) {
                val (a1, b1, c1) = filas[i]
                val (a2, b2, c2) = filas[j]
                val det = a1 * b2 - a2 * b1
                if (abs(det) < 1e-12) continue
                val lnV = (c1 * b2 - c2 * b1) / det
                val lnI = (a1 * c2 - a2 * c1) / det
                return wa * lnV + wb * lnI + off
            }
            // Una sola dirección conocida: la salida debe ser proporcional a ella.
            for ((a, b, c) in filas) {
                if (abs(a * wb - b * wa) < 1e-12) {
                    val lambda = if (a != 0.0) wa / a else wb / b
                    return lambda * c + off
                }
            }
            throw ErrorCalculo(MotivoError.DATOS_INSUFICIENTES)
        }
    }
}
