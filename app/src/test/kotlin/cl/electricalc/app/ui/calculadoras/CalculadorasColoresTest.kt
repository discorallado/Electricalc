package cl.electricalc.app.ui.calculadoras

import cl.electricalc.calc.comun.SistemaElectrico
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Locale

class CalculadorasColoresTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun idioma() = Locale.setDefault(Locale.US)
    }

    @Test
    fun `smd lee el codigo de texto`() {
        val e = Entrada(SistemaElectrico.MONOFASICA, null, emptyMap(), emptyMap(), mapOf("letra" to 0), mapOf("codigo" to " 4k7 "))
        assertEquals("4.7 kΩ", CalculadoraSmd.smd.calcular(e).first().valor)
        val vacio = Entrada(SistemaElectrico.MONOFASICA, null, emptyMap(), emptyMap(), mapOf("letra" to 0), mapOf("codigo" to ""))
        assertThrows<FaltanDatos> { CalculadoraSmd.smd.calcular(vacio) }
    }

    @Test
    fun `lectura de 4 bandas amarillo violeta rojo dorado`() {
        val c = CalculadorasColores.lectura
        val cuatro = c.variantes(SistemaElectrico.MONOFASICA).first()
        val e = Entrada(SistemaElectrico.MONOFASICA, cuatro, emptyMap(), emptyMap(), mapOf("c1" to 4, "c2" to 7, "mult" to 2, "tol4" to 0))
        val r = c.calcular(e)
        assertEquals("4.7 kΩ", r[0].valor)
        assertEquals("± 5 %", r[1].valor)
    }
}
