package cl.electricalc.app.datos

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProyectoTest {
    private val casa = Proyecto(
        nombre = "Casa",
        cargas = listOf(
            Carga("Iluminación", TipoCarga.ILUMINACION, 100.0, 20),
            Carga("Enchufes", TipoCarga.ENCHUFES, 250.0, 16),
            Carga("Bomba", TipoCarga.FUERZA, 2000.0),
        ),
    )

    @Test
    fun `demanda, corriente, empalme y alimentador de una casa`() {
        val r = CalculoProyecto.calcular(casa)
        assertEquals(8000.0, r.instaladaW, 1e-9)
        // Alumbrado 6 kW: 3 × 1 + 3 × 0,35 = 4,05 kW; más 2 kW de fuerza.
        assertEquals(6050.0, r.demandaW, 1e-9)
        // 4.050/220 + 1,25 × 2.000/220 = 29,77 A.
        assertEquals(29.77, r.corrienteA, 0.01)
        // 6,05 kW en monofásica: el primero que cubre es 32 A / 6,5 kW.
        assertEquals(32.0, r.empalme!!.a)
        // 4 mm² B1 a 70 °C da 28 A < 29,8: sube a 5,26 mm².
        assertEquals(5.26, r.alimentador!!.mm2)
        assertFalse(r.memoriaObligatoria)
        assertEquals(7, r.calculosMemoria.size)
    }

    @Test
    fun `sobre 20 kW la memoria pide calculos adicionales`() {
        val grande = casa.copy(cargas = casa.cargas + Carga("Clima", TipoCarga.CLIMATIZACION, 15000.0), trifasico = true, tensionV = 380.0)
        val r = CalculoProyecto.calcular(grande)
        assertTrue(r.memoriaObligatoria)
        assertEquals(10, r.calculosMemoria.size)
    }

    @Test
    fun `ida y vuelta en JSON`() {
        assertEquals(casa, Proyecto.deJson(casa.aJson()))
    }
}
