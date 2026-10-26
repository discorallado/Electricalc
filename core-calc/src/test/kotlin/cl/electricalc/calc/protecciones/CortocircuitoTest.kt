package cl.electricalc.calc.protecciones

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot
import kotlin.math.sqrt

class CortocircuitoTest {
    @Test
    fun energiaYSeccion() {
        assertEquals(10000.0 * 10000.0, Cortocircuito.energiaAdmisible(100.0, 100.0), 1e-3)
        assertEquals(1.0, Cortocircuito.tiempoMaximo(100.0, 10.0, 1000.0), 1e-12)
        assertEquals(10.0, Cortocircuito.seccionMinima(1000.0, 1.0, 100.0), 1e-12)
    }

    @Test
    fun transformadorYLinea() {
        val icc = Cortocircuito.transformador(630e3, 400.0, 0.04)
        assertEquals(630e3 / (sqrt(3.0) * 400 * 0.04), icc, 1e-6)
        assertTrue(Cortocircuito.transformador(630e3, 400.0, 0.04, 500e6) < icc)
        assertEquals(icc, Cortocircuito.aguasAbajo(icc, 400.0, 0.0, 0.0), 1e-6)
        val abajo = Cortocircuito.aguasAbajo(10000.0, 400.0, 0.05, 0.01)
        assertEquals(400 / (sqrt(3.0) * hypot(400 / (sqrt(3.0) * 10000) + 0.05, 0.01)), abajo, 1e-9)
    }

    @Test
    fun minimaYLargo() {
        val l = Cortocircuito.largoMaximo(230.0, 100.0, 1.83, 0.08, 6000.0)
        val r = 1.83 * l
        val x = 0.08 * l
        assertEquals(100.0, Cortocircuito.minima(6000.0, 230.0, r, x), 1e-6)
        assertEquals(0.0, Cortocircuito.largoMaximo(230.0, 100.0, 1.0, 0.0, 50.0), 1e-12)
        assertTrue(Coordinacion.cumple(14.0, 16.0, 21.0))
        assertFalse(Coordinacion.cumple(14.0, 25.0, 21.0))
    }
}
