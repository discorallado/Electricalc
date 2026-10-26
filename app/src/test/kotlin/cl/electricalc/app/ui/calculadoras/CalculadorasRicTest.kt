package cl.electricalc.app.ui.calculadoras

import cl.electricalc.calc.comun.SistemaElectrico
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Locale

class CalculadorasRicTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun idioma() = Locale.setDefault(Locale.US)
    }

    private fun calcular(calc: Calculadora, valores: Map<String, Double>, opciones: Map<String, Int>) =
        calc.calcular(
            Entrada(SistemaElectrico.MONOFASICA, null, valores, valores.keys.associateWith { Unidad("") }, opciones),
        ).map { it.valor }

    @Test
    fun `capacidad corregida con fn y ft`() {
        // 90 °C, B1, 2,5 mm² (fila 2), 40 °C, 4 conductores: 24 × 0,8 × 0,91 = 17,5 A.
        val r = calcular(
            CalculadorasRic.capacidad, mapOf("t_ambiente" to 40.0, "conductores" to 4.0),
            mapOf("aislacion" to 1, "metodo" to 2, "seccion_tabla" to 2),
        )
        assertEquals(listOf("17.5 A", "24 A", "0.8", "0.91"), r)
    }

    @Test
    fun `circuito de iluminacion con potencia por defecto`() {
        // 8 centros × 100 W = 800 W; ×1,1/220 = 4 A → 6 A curva B; 1,5 mm² (70 °C B1: 16 A); 0,9·6·220/100 = 11 centros.
        val r = calcular(
            CalculadorasRic.circuitosVivienda,
            mapOf(Ids.TENSION to 220.0, "centros" to 8.0, "t_ambiente" to 30.0, "conductores" to 3.0),
            mapOf("tipo_circuito" to 0, "aislacion" to 0, "metodo" to 2),
        )
        assertEquals(listOf("6 A, curva B", "1.5 mm²", "≤ 30 mA", "800 W", "4 A", "11", "16 A", "≤ 3"), r)
    }

    @Test
    fun `circuito de cocina exige 16 A y 2,5 mm2`() {
        val r = calcular(
            CalculadorasRic.circuitosVivienda,
            mapOf(Ids.TENSION to 220.0, "centros" to 4.0, "t_ambiente" to 30.0, "conductores" to 3.0),
            mapOf("tipo_circuito" to 2, "aislacion" to 0, "metodo" to 2),
        )
        assertEquals("16 A, curva C", r[0])
        assertEquals("2.5 mm²", r[1])
    }
}
