package cl.electricalc.app.ui.calculadoras

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.ohm.Magnitud
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.math.sqrt

/**
 * Recorre cada calculadora con todos sus tipos de corriente, variantes de datos conocidos y unidades de
 * potencia. Los datos salen de un circuito de referencia coherente, así que cada combinación debe
 * devolver el valor del circuito. Si una pantalla ocultara el cosφ cuando hace falta, el resultado no
 * coincidiría.
 */
class CalculadorasTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun idioma() = Locale.setDefault(Locale.US)

        const val COS = 0.8
    }

    /** Circuito con V = 230 V, I = 7 A y cosφ = 0,8 (1 en continua). */
    private fun circuito(s: SistemaElectrico): Map<Magnitud, Double> {
        val cos = if (s.esAlterna) COS else 1.0
        val v = 230.0
        val i = 7.0
        val sAp = s.factor * v * i
        return mapOf(
            Magnitud.TENSION to v,
            Magnitud.CORRIENTE to i,
            Magnitud.IMPEDANCIA to v / i,
            Magnitud.RESISTENCIA to v / i * cos,
            Magnitud.POTENCIA_APARENTE to sAp,
            Magnitud.POTENCIA_ACTIVA to sAp * cos,
            Magnitud.POTENCIA_REACTIVA to sAp * sqrt(1 - cos * cos),
        )
    }

    private val salidas = mapOf(
        CalculadorasOhm.corriente to (Magnitud.CORRIENTE to "A"),
        CalculadorasOhm.tension to (Magnitud.TENSION to "V"),
        CalculadorasOhm.potenciaActiva to (Magnitud.POTENCIA_ACTIVA to "W"),
        CalculadorasOhm.potenciaAparente to (Magnitud.POTENCIA_APARENTE to "VA"),
        CalculadorasOhm.potenciaReactiva to (Magnitud.POTENCIA_REACTIVA to "var"),
        CalculadorasOhm.resistencia to (Magnitud.RESISTENCIA to "Ω"),
        CalculadorasOhm.impedancia to (Magnitud.IMPEDANCIA to "Ω"),
    )

    /** Unidad base de cada campo; la potencia genérica se prueba con [potencia]. */
    private fun unidadBase(campo: CampoNumerico, s: SistemaElectrico, potencia: Unidad): Unidad =
        if (campo.id == Ids.POTENCIA) potencia else campo.unidades(s).first { it.aBase(1.0) == 1.0 }

    /** Ejecuta una combinación y devuelve los resultados y si se pidió el cosφ. */
    private fun ejecutar(
        calc: Calculadora,
        s: SistemaElectrico,
        variante: Variante,
        potencia: Unidad,
    ): Pair<List<LineaResultado>, Boolean> {
        val numericos = calc.campos.filterIsInstance<CampoNumerico>()
        val unidades = numericos.associate { it.id to unidadBase(it, s, potencia) }
        val real = circuito(s)
        val visibles = numericos.filter { calc.visible(it, s, variante, unidades) }
        val valores = visibles.associate { c ->
            c.id to if (c.id == Ids.FASE) COS else real.getValue(c.magnitud ?: unidades.getValue(c.id).magnitud!!)
        }
        val resultado = calc.calcular(Entrada(s, variante, valores, unidades, emptyMap()))
        return resultado to visibles.any { it.id == Ids.FASE }
    }

    private fun unidadesPotencia(s: SistemaElectrico) =
        if (s.esAlterna) listOf(Unidades.W, Unidades.VA, Unidades.varU) else listOf(Unidades.W)

    @Test
    fun `cada combinacion de ley de Ohm devuelve el valor del circuito`() {
        var casos = 0
        for ((calc, salida) in salidas) {
            val (magnitud, simbolo) = salida
            for (s in calc.sistemas) for (variante in calc.variantes(s)) {
                val potencias = if (Ids.POTENCIA in variante.campos) unidadesPotencia(s) else listOf(Unidades.W)
                for (potencia in potencias) {
                    val (lineas, _) = ejecutar(calc, s, variante, potencia)
                    val esperado = Formato.conPrefijo(circuito(s).getValue(magnitud), simbolo)
                    assertEquals(
                        esperado, lineas.first().valor,
                        "${calc.id} $s ${variante.campos} ${potencia.simbolo}",
                    )
                    casos++
                }
            }
        }
        assertTrue(casos > 100, "Se esperaban más de 100 combinaciones, hubo $casos")
    }

    @Test
    fun `el factor de potencia se recupera con todas las variantes`() {
        val calc = CalculadorasOhm.factorPotencia
        for (s in calc.sistemas) for (variante in calc.variantes(s)) {
            val (lineas, pideCos) = ejecutar(calc, s, variante, Unidades.W)
            assertEquals("0.8", lineas[0].valor, "$s ${variante.campos}")
            assertEquals("36.87°", lineas[1].valor, "$s ${variante.campos}")
            assertEquals(false, pideCos, "El factor de potencia no puede pedirse como dato")
        }
    }

    @Test
    fun `el cosφ solo se pide cuando cambia el resultado`() {
        val calc = CalculadorasOhm.corriente
        val s = SistemaElectrico.MONOFASICA
        val pr = calc.variantes(s).first { it.campos == listOf(Ids.POTENCIA, Ids.RESISTENCIA) }
        assertEquals(false, ejecutar(calc, s, pr, Unidades.W).second, "I = √(P/R) no depende del cosφ")
        assertEquals(true, ejecutar(calc, s, pr, Unidades.VA).second, "I = √(S·cosφ/R) sí depende")
        val vz = calc.variantes(s).first { it.campos == listOf(Ids.TENSION, Ids.IMPEDANCIA) }
        assertEquals(false, ejecutar(calc, s, vz, Unidades.W).second, "I = U/Z no depende del cosφ")
        val cc = calc.variantes(SistemaElectrico.CONTINUA).first()
        assertEquals(false, ejecutar(calc, SistemaElectrico.CONTINUA, cc, Unidades.W).second, "En continua no hay cosφ")
    }

    @Test
    fun `caida de tension trifasica coincide con el calculo a mano`() {
        // 3000 W, 380 V, cosφ 0,9, 2,5 mm² Cu unipolar, 50 m, 70 °C:
        // I = 3000/(√3·380·0,9) = 5,064 A; R = 7,98·(1 + 0,00393·50) Ω/km; X = 0,132 Ω/km
        // ΔV = √3·I·0,05·(R·0,9 + X·0,436) = 3,794 V
        val calc = CalculadoraCaida.caida
        val unidades = mapOf(
            Ids.TENSION to Unidades.V, Ids.CARGA to Unidades.W, Ids.FASE to Unidades.cosPhi,
            Ids.SECCION to Unidad(CalculadoraCaida.MM2), Ids.LONGITUD to Unidades.m, Ids.TEMPERATURA to Unidades.celsius,
        )
        val valores = mapOf(
            Ids.TENSION to 380.0, Ids.CARGA to 3000.0, Ids.FASE to 0.9, Ids.LONGITUD to 50.0, Ids.TEMPERATURA to 70.0,
        )
        val opciones = mapOf(Ids.SECCION to 4, Ids.PARALELOS to 0, Ids.MATERIAL to 0, Ids.TIPO_CABLE to 0)
        val r = calc.calcular(Entrada(SistemaElectrico.TRIFASICA, null, valores, unidades, opciones))
        assertEquals(listOf("3.794 V", "1.00 %", "376.206 V", "5.064 A"), r.map { it.valor })
    }

    @Test
    fun `caida de tension oculta el cosφ en continua y acepta la carga en amperes`() {
        val calc = CalculadoraCaida.caida
        val cos = calc.campos.first { it.id == Ids.FASE }
        assertEquals(false, calc.visible(cos, SistemaElectrico.CONTINUA, null, emptyMap()))
        assertEquals(true, calc.visible(cos, SistemaElectrico.MONOFASICA, null, emptyMap()))
        // CC, 10 A, 2,5 mm² Cu a 20 °C, 10 m: ΔV = 2·10·0,01·7,98 = 1,596 V
        val unidades = mapOf(
            Ids.TENSION to Unidades.V, Ids.CARGA to Unidades.A,
            Ids.SECCION to Unidad(CalculadoraCaida.MM2), Ids.LONGITUD to Unidades.m, Ids.TEMPERATURA to Unidades.celsius,
        )
        val valores = mapOf(Ids.TENSION to 24.0, Ids.CARGA to 10.0, Ids.LONGITUD to 10.0, Ids.TEMPERATURA to 20.0)
        val opciones = mapOf(Ids.SECCION to 4, Ids.PARALELOS to 0, Ids.MATERIAL to 0, Ids.TIPO_CABLE to 1)
        val r = calc.calcular(Entrada(SistemaElectrico.CONTINUA, null, valores, unidades, opciones))
        assertEquals("1.596 V", r[0].valor)
    }

    /** Datos de conductor comunes: 2,5 mm² Cu unipolar, un conductor por fase, 70 °C. */
    private val conductor = mapOf(Ids.SECCION to 4, Ids.PARALELOS to 0, Ids.MATERIAL to 0, Ids.TIPO_CABLE to 0)
    private val unidadesLinea = mapOf(
        Ids.TENSION to Unidades.V, Ids.CARGA to Unidades.W, Ids.FASE to Unidades.cosPhi,
        Ids.SECCION to Unidad(CalculadoraCaida.MM2), Ids.LONGITUD to Unidades.m, Ids.TEMPERATURA to Unidades.celsius,
        Ids.NUMERO_CARGAS to Unidad(""), Ids.CAIDA_MAXIMA to Unidad(CalculadoraCaida.PORCENTAJE),
    )

    @Test
    fun `cargas distribuidas coincide con el calculo a mano`() {
        // 10 luminarias de 100 W en monofásica 220 V, cosφ 1, 100 m de 2,5 mm² Cu a 70 °C:
        // I = 100/220 = 0,4545 A; R = 7,98·(1 + 0,00393·50) = 9,548 Ω/km
        // ΔV = 2·I·0,1 km·R·(10 + 1)/2 = 4,774 V
        val valores = mapOf(
            Ids.TENSION to 220.0, Ids.CARGA to 100.0, Ids.NUMERO_CARGAS to 10.0, Ids.FASE to 1.0,
            Ids.LONGITUD to 100.0, Ids.TEMPERATURA to 70.0,
        )
        val r = CalculadoraCaida.cargasDistribuidas.calcular(
            Entrada(SistemaElectrico.MONOFASICA, null, valores, unidadesLinea, conductor),
        )
        assertEquals(listOf("4.774 V", "2.17 %", "215.226 V", "10 m", "454.545 mA", "4.545 A"), r.map { it.valor })
    }

    @Test
    fun `con una sola carga distribuida es la caida con la carga al final`() {
        val valores = mapOf(
            Ids.TENSION to 380.0, Ids.CARGA to 3000.0, Ids.NUMERO_CARGAS to 1.0, Ids.FASE to 0.9,
            Ids.LONGITUD to 50.0, Ids.TEMPERATURA to 70.0,
        )
        val s = SistemaElectrico.TRIFASICA
        val una = CalculadoraCaida.cargasDistribuidas.calcular(Entrada(s, null, valores, unidadesLinea, conductor))
        val alFinal = CalculadoraCaida.caida.calcular(Entrada(s, null, valores, unidadesLinea, conductor))
        assertEquals(alFinal.take(3).map { it.valor }, una.take(3).map { it.valor })
    }

    @Test
    fun `el numero de cargas debe ser entero y positivo`() {
        for (n in listOf(0.0, 2.5, -3.0)) {
            val valores = mapOf(
                Ids.TENSION to 220.0, Ids.CARGA to 100.0, Ids.NUMERO_CARGAS to n, Ids.FASE to 1.0,
                Ids.LONGITUD to 100.0, Ids.TEMPERATURA to 70.0,
            )
            val error = runCatching {
                CalculadoraCaida.cargasDistribuidas.calcular(Entrada(SistemaElectrico.MONOFASICA, null, valores, unidadesLinea, conductor))
            }.exceptionOrNull()
            assertTrue(error is ErrorCalculo && error.motivo == MotivoError.VALOR_FUERA_DE_RANGO, "n = $n")
        }
    }

    @Test
    fun `la longitud maxima produce exactamente la caida pedida`() {
        for (s in SistemaElectrico.entries) {
            val base = mapOf(Ids.TENSION to 380.0, Ids.CARGA to 3000.0, Ids.FASE to 0.9, Ids.TEMPERATURA to 70.0)
            val enPorcentaje = CalculadoraCaida.longitudMaxima.calcular(
                Entrada(s, null, base + (Ids.CAIDA_MAXIMA to 3.0), unidadesLinea, conductor),
            )
            // 3 % de 380 V = 11,4 V: ingresarlo en volts da la misma longitud.
            val enVolts = CalculadoraCaida.longitudMaxima.calcular(
                Entrada(s, null, base + (Ids.CAIDA_MAXIMA to 11.4), unidadesLinea + (Ids.CAIDA_MAXIMA to Unidades.V), conductor),
            )
            assertEquals(enPorcentaje.map { it.valor }, enVolts.map { it.valor }, "$s")

            val metros = Formato.leer(enPorcentaje[0].valor.removeSuffix(" m").replace(",", ""))!!
            val caida = CalculadoraCaida.caida.calcular(
                Entrada(s, null, base + (Ids.LONGITUD to metros), unidadesLinea, conductor),
            )
            assertEquals("3.00 %", caida[1].valor, "$s: la caída con la longitud máxima debe ser la pedida")
        }
    }

    @Test
    fun `las unidades convierten a la base`() {
        assertEquals(1500.0, Unidades.kW.aBase(1.5), 1e-9)
        assertEquals(745.699872, Unidades.HP.aBase(1.0), 1e-9)
        assertEquals(30.48, Unidades.ft.aBase(100.0), 1e-9)
        assertEquals(70.0, Unidades.fahrenheit.aBase(158.0), 1e-9)
        assertEquals(0.8, Unidades.senPhi.aBase(0.6), 1e-12)
        assertEquals(0.8, Unidades.tanPhi.aBase(0.75), 1e-12)
        assertEquals(0.5, Unidades.phiGrados.aBase(60.0), 1e-12)
        assertTrue(Unidades.senPhi.aBase(1.5).isNaN())
    }
}
