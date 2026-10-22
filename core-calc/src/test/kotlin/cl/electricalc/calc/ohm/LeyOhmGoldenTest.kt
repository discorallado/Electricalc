package cl.electricalc.calc.ohm

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.SistemaElectrico
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * Compara contra vectores generados con la app de referencia (docs/plan.md, Fase 3).
 * Donde la referencia entrega un valor, el nuestro debe coincidir; donde falla por un valor
 * inválido (p. ej. Q = 0 con cosφ = 1), el nuestro también debe fallar.
 */
class LeyOhmGoldenTest {

    private val casos = Json.parseToJsonElement(
        javaClass.getResource("/golden/ohm.json")!!.readText(),
    ).jsonArray.map { it.jsonObject }

    private val sistemas = mapOf(
        "CONTINUA" to SistemaElectrico.CONTINUA,
        "MONOFASE" to SistemaElectrico.MONOFASICA,
        "BIFASE" to SistemaElectrico.BIFASICA,
        "TRIFASE" to SistemaElectrico.TRIFASICA,
    )

    private val magnitudes = mapOf(
        "V" to Magnitud.TENSION,
        "I" to Magnitud.CORRIENTE,
        "R" to Magnitud.RESISTENCIA,
        "Z" to Magnitud.IMPEDANCIA,
        "P" to Magnitud.POTENCIA_ACTIVA,
        "Q" to Magnitud.POTENCIA_REACTIVA,
        "S" to Magnitud.POTENCIA_APARENTE,
    )

    private fun ejecutar(caso: JsonObject): Result<Double> {
        val datos = LeyOhm.Datos(
            sistema = sistemas.getValue(caso["tipo"]!!.jsonPrimitive.content),
            conocidas = caso["entradas"]!!.jsonObject.entries
                .associate { (k, v) -> magnitudes.getValue(k) to v.jsonPrimitive.double },
            cosPhi = caso["cosPhi"]!!.jsonPrimitive.double,
        )
        val salida = caso["salida"]!!.jsonPrimitive.content
        return runCatching {
            if (salida == "cosPhi") LeyOhm.calcularCosPhi(datos)
            else LeyOhm.calcular(datos, magnitudes.getValue(salida))
        }
    }

    @Test
    fun `coincide con la referencia donde esta entrega un valor`() {
        val conValor = casos.filter { it["valor"]?.jsonPrimitive?.doubleOrNull != null }
        assertTrue(conValor.size > 1500)
        val fallos = conValor.mapNotNull { caso ->
            val esperado = caso["valor"]!!.jsonPrimitive.double
            val obtenido = ejecutar(caso)
            val ok = obtenido.getOrNull()?.let { abs(it - esperado) <= 1e-9 * maxOf(1.0, abs(esperado)) } ?: false
            if (ok) null else "$caso -> $obtenido"
        }
        assertEquals(emptyList<String>(), fallos.take(20), "fallos: ${fallos.size}")
    }

    @Test
    fun `falla donde la referencia rechaza un valor invalido`() {
        val invalidos = casos.filter {
            val e = it["error"]?.jsonPrimitive?.content
            // Con cosφ = 1 exacto la referencia a veces obtiene 1.0000000000000002 y lo rechaza por redondeo.
            val redondeo = e == "cosphi_non_valido" && it["cosPhi"]!!.jsonPrimitive.double == 1.0
            e != null && e != "no_soportado" && !redondeo
        }
        val fallos = invalidos.filter { ejecutar(it).exceptionOrNull() !is ErrorCalculo }
        assertEquals(emptyList<JsonObject>(), fallos.take(20), "fallos: ${fallos.size}")
    }
}
