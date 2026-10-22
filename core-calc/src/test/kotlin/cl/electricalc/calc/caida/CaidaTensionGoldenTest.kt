package cl.electricalc.calc.caida

import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.conductores.Cable
import cl.electricalc.calc.conductores.Material
import cl.electricalc.calc.conductores.Seccion
import cl.electricalc.calc.conductores.TipoCable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs

class CaidaTensionGoldenTest {

    private val casos = Json.parseToJsonElement(
        javaClass.getResource("/golden/caida.json")!!.readText(),
    ).jsonArray.map { it.jsonObject }

    private val sistemas = mapOf(
        "CONTINUA" to SistemaElectrico.CONTINUA,
        "MONOFASE" to SistemaElectrico.MONOFASICA,
        "BIFASE" to SistemaElectrico.BIFASICA,
        "TRIFASE" to SistemaElectrico.TRIFASICA,
    )

    private fun cerca(a: Double, b: Double) = abs(a - b) <= 1e-9 * maxOf(1.0, abs(b))

    @Test
    fun `coincide con la referencia`() {
        val fallos = casos.mapNotNull { c ->
            fun d(k: String) = c[k]!!.jsonPrimitive.double
            val indice = c["indiceSeccion"]!!.jsonPrimitive.int
            val cable = Cable(
                seccion = if (c["awg"]!!.jsonPrimitive.boolean) Seccion.Awg(indice) else Seccion.Metrica(indice),
                material = if (c["material"]!!.jsonPrimitive.content == "RAME") Material.COBRE else Material.ALUMINIO,
                tipo = TipoCable.valueOf(c["cable"]!!.jsonPrimitive.content),
                paralelos = c["paralelos"]!!.jsonPrimitive.int,
                longitudM = d("longitud"),
                temperatura = d("temperatura"),
            )
            val r = CaidaTension.calcular(
                sistemas.getValue(c["tipo"]!!.jsonPrimitive.content),
                d("tension"), d("corriente"), d("cosPhi"), cable, c["cargas"]!!.jsonPrimitive.int,
            )
            val ok = cerca(r.caidaV, d("caida")) && cerca(r.porcentaje, d("porcentaje")) && cerca(r.tensionFinal, d("tensionFinal"))
            if (ok) null else "$c -> $r"
        }
        assertEquals(emptyList<String>(), fallos.take(10), "fallos: ${fallos.size} de ${casos.size}")
    }
}
