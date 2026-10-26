package cl.electricalc.calc.energia

import cl.electricalc.calc.comun.ErrorCalculo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EnergiaTest {
    @Test
    fun consumo() {
        assertEquals(36.0, Consumo.kwh(1200.0, 1.0, 30.0), 1e-9)
        assertThrows<ErrorCalculo> { Consumo.kwh(100.0, 25.0, 1.0) }
    }

    @Test
    fun lumenes() {
        // Oficina 6 × 4 m, h = 2,2 m: K = 24 / (2,2 × 10) = 1,09.
        assertEquals(1.0909, Lumenes.indiceLocal(6.0, 4.0, 2.2), 1e-4)
        // 500 lx × 24 m² / (0,6 × 0,8) = 25.000 lm → con luminarias de 3.600 lm: 7.
        val flujo = Lumenes.flujoTotal(500.0, 24.0, 0.6, 0.8)
        assertEquals(25000.0, flujo, 1e-9)
        assertEquals(7, Lumenes.luminarias(flujo, 3600.0))
        assertEquals(504.0, Lumenes.iluminanciaObtenida(7, 3600.0, 24.0, 0.6, 0.8), 1e-9)
    }

    @Test
    fun compensacion() {
        // 100 kW de 0,8 a 0,95: 100 × (0,75 − 0,3287) = 42,13 kvar.
        assertEquals(42131.0, Compensacion.reactiva(100000.0, 0.8, 0.95), 1.0)
        assertThrows<ErrorCalculo> { Compensacion.reactiva(1000.0, 0.95, 0.8) }
        // 42,13 kvar a 380 V, 50 Hz: C = Q / (2π f U²) = 929 µF por fase.
        assertEquals(928.7e-6, Compensacion.capacidadEstrella(42131.0, 380.0), 1e-7)
    }

    @Test
    fun respaldo() {
        // 4 kW, cosφ 0,8, 25 %: 6.250 VA.
        assertEquals(6250.0, Respaldo.potenciaAparente(4000.0, 0.8, 0.25), 1e-9)
        // 1 kW × 2 h / (48 V × 0,9 × 0,5) = 92,6 Ah.
        assertEquals(92.59, Respaldo.capacidadBaterias(1000.0, 2.0, 48.0, 0.9, 0.5), 0.01)
    }
}
