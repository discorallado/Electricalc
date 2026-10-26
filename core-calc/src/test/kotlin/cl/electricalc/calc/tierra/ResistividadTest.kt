package cl.electricalc.calc.tierra

import cl.electricalc.calc.comun.ErrorCalculo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.PI

class ResistividadTest {

    /** Expresión general de cuatro electrodos en línea, para comparar las dos disposiciones. */
    private fun general(am: Double, bm: Double, an: Double, bn: Double, r: Double) =
        2 * PI * r / (1 / am - 1 / bm - 1 / an + 1 / bn)

    @Test
    fun `wenner es la expresion general con separacion pareja`() {
        // a = 4 m, R = 2 Ω → ρ = 2π·4·2 = 50,27 Ω·m.
        assertEquals(50.265, Resistividad.wenner(4.0, 2.0), 1e-3)
        assertEquals(general(4.0, 8.0, 8.0, 4.0, 2.0), Resistividad.wenner(4.0, 2.0), 1e-9)
    }

    @Test
    fun `wenner profundo tiende a wenner con poca profundidad`() {
        assertEquals(Resistividad.wenner(10.0, 3.0), Resistividad.wennerProfundo(10.0, 1e-6, 3.0), 1e-6)
        // Con b = a/20 la corrección ya es chica pero visible.
        val profundo = Resistividad.wennerProfundo(10.0, 0.5, 3.0)
        assertEquals(Resistividad.wenner(10.0, 3.0), profundo, Resistividad.wenner(10.0, 3.0) * 0.01)
    }

    @Test
    fun `schlumberger es la expresion general con MN centrado`() {
        // AB/2 = 10 m, MN/2 = 1 m: AM = BN = 9, BM = AN = 11.
        assertEquals(general(9.0, 11.0, 11.0, 9.0, 0.5), Resistividad.schlumberger(10.0, 1.0, 0.5), 1e-9)
        // π·0,5·(100 − 1)/2 = 77,75 Ω·m.
        assertEquals(77.754, Resistividad.schlumberger(10.0, 1.0, 0.5), 1e-3)
    }

    @Test
    fun `rechaza geometrias imposibles`() {
        assertThrows<ErrorCalculo> { Resistividad.schlumberger(1.0, 1.0, 1.0) }
        assertThrows<ErrorCalculo> { Resistividad.wenner(0.0, 1.0) }
        assertThrows<ErrorCalculo> { Resistividad.wenner(1.0, -2.0) }
    }
}
