package cl.electricalc.app.ui.calculadoras

import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.datos.ric.EstadoVerificacion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Locale

class CalculadorasProteccionTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun idioma() = Locale.setDefault(Locale.US)
    }

    private fun entrada(valores: Map<String, Double>, opciones: Map<String, Int>) =
        Entrada(SistemaElectrico.MONOFASICA, null, valores, valores.keys.associateWith { Unidad("") }, opciones)

    @Test
    fun `selector de diferencial`() {
        val c = CalculadorasProteccion.selector
        assertEquals(listOf("AC o A", "≤ 30 mA", "RIC N°10, numeral 5.1.3.5"), c.calcular(entrada(emptyMap(), mapOf("aplicacion" to 0))).map { it.valor })
        assertEquals("B", c.calcular(entrada(emptyMap(), mapOf("aplicacion" to 6))).first().valor)
        assertEquals("RIC N°15, numeral 12.5.5", c.calcular(entrada(emptyMap(), mapOf("aplicacion" to 7)))[2].valor)
    }

    @Test
    fun `verificacion TT`() {
        val c = CalculadorasProteccion.tt
        // 40 Ω × 30 mA = 1,2 V ≤ 50 V; RA máx = 50/0,03 = 1.667 Ω.
        val ok = entrada(mapOf("ra" to 40.0, "sensibilidad" to 0.03), mapOf("lugar" to 0, "corriente_tipo" to 0))
        assertEquals(listOf("1.2 V", "1.667 kΩ", "50 V"), c.calcular(ok).map { it.valor })
        assertEquals(EstadoVerificacion.CUMPLE, c.verificar(ok).single().estado)
        // 1.000 Ω × 30 mA = 30 V > 24 V en lugar húmedo.
        val mal = entrada(mapOf("ra" to 1000.0, "sensibilidad" to 0.03), mapOf("lugar" to 1, "corriente_tipo" to 0))
        assertEquals(EstadoVerificacion.NO_CUMPLE, c.verificar(mal).single().estado)
    }
}
