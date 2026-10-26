package cl.electricalc.app.ui.calculadoras

import cl.electricalc.calc.comun.SistemaElectrico
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Locale

class CalculadorasTierraTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun idioma() = Locale.setDefault(Locale.US)
    }

    private val s = SistemaElectrico.MONOFASICA

    private fun calcular(calc: Calculadora, variante: Int, valores: Map<String, Double>, opciones: Map<String, Int> = emptyMap()) =
        calc.calcular(
            Entrada(
                s, calc.variantes(s)[variante], valores,
                valores.keys.associateWith { Unidad("") }, opciones,
            ),
        ).map { it.valor }

    @Test
    fun `resistividad por wenner y schlumberger`() {
        val c = CalculadorasTierra.resistividad
        // Wenner: a = 4 m, R = 2 Ω → 2π·4·2 = 50,27 Ω·m; ala = 1,5·a = 6 m.
        assertEquals(listOf("50.27 Ω·m", "6 m"), calcular(c, 0, mapOf("separacion" to 4.0, "resistencia_medida" to 2.0)))
        // Schlumberger: AB/2 = 10, MN/2 = 1, R = 0,5 → 77,75 Ω·m; ala = AB/2.
        assertEquals(
            listOf("77.75 Ω·m", "10 m"),
            calcular(c, 2, mapOf("semi_corriente" to 10.0, "semi_potencial" to 1.0, "resistencia_medida" to 0.5)),
        )
    }

    @Test
    fun `resistencia de electrodos con la tabla 6_4`() {
        val c = CalculadorasTierra.electrodosTierra
        // Pica (opción 1) de 2 m en 100 Ω·m: R = ρ/L = 50 Ω.
        assertEquals("50 Ω", calcular(c, 0, mapOf("resistividad" to 100.0, "medida" to 2.0), mapOf("electrodo" to 1))[0])
        // Placa (opción 0) de 4 m de perímetro en terreno poco fértil (500 Ω·m, tabla 6.3): 0,8·500/4 = 100 Ω.
        assertEquals("100 Ω", calcular(c, 1, mapOf("medida" to 4.0), mapOf("electrodo" to 0, "terreno" to 1))[0])
        // Conductor horizontal (opción 2) de 20 m en 100 Ω·m: 2ρ/L = 10 Ω.
        assertEquals("10 Ω", calcular(c, 0, mapOf("resistividad" to 100.0, "medida" to 20.0), mapOf("electrodo" to 2))[0])
    }
}
