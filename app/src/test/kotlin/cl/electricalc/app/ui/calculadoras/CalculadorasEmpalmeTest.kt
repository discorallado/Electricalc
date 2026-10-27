package cl.electricalc.app.ui.calculadoras

import cl.electricalc.calc.comun.SistemaElectrico
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Locale

class CalculadorasEmpalmeTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun idioma() = Locale.setDefault(Locale.US)
    }

    private fun calcular(c: Calculadora, s: SistemaElectrico, valores: Map<String, Double>, opciones: Map<String, Int>) =
        c.calcular(Entrada(s, null, valores, valores.keys.associateWith { Unidad("") }, opciones)).map { if (it.plantilla != null) it.args.joinToString("|") else it.valor }

    @Test
    fun `empalme monofasico de vivienda con minimo de 25 A`() {
        // 3 kW de demanda: 16 A cubriría, pero una vivienda necesita 25 A.
        val r = calcular(CalculadorasEmpalme.empalme, SistemaElectrico.MONOFASICA, mapOf("instalada" to 8000.0, "demanda" to 3000.0), mapOf("vivienda" to 1))
        assertEquals("25 A · 5 kW", r[0])
        val local = calcular(CalculadorasEmpalme.empalme, SistemaElectrico.TRIFASICA, mapOf("instalada" to 30000.0, "demanda" to 25000.0), mapOf("vivienda" to 0))
        assertEquals("50 A · 30 kW", local[0])
    }

    @Test
    fun `carga de VE modo 3 monofasico`() {
        // 7,4 kW a 220 V: 33,6 A; protección 1,25 × 33,6 = 42 A → 50 A; diferencial tipo B; sin SGC demanda = 7,4 kW.
        val r = calcular(
            CalculadorasEmpalme.cargaVe, SistemaElectrico.MONOFASICA,
            mapOf(Ids.TENSION to 220.0, "potencia_save" to 7400.0, "cantidad" to 1.0, "empalme_kw" to 10000.0),
            mapOf("modo" to 2, "tipo_irve" to 0, "sgc" to 0),
        )
        assertEquals(listOf("33.6 A", "50 A (1.25 × In)", "B|30", "7.4 kW", "2.6 kW"), r.take(5))
    }
}
