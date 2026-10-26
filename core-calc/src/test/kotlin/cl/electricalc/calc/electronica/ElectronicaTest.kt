package cl.electricalc.calc.electronica

import cl.electricalc.calc.comun.ErrorCalculo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ElectronicaTest {
    @Test
    fun reactancias() {
        assertEquals(31.4159, Reactancia.inductiva(50.0, 0.1), 1e-4)
        assertEquals(31.831, Reactancia.capacitiva(50.0, 100e-6), 1e-3)
        assertEquals(5.0, Reactancia.impedancia(3.0, 4.0), 1e-12)
        assertEquals(53.13, Reactancia.angulo(3.0, 4.0), 1e-2)
        assertEquals(5032.9, Reactancia.resonancia(1e-3, 1e-6), 0.1)
        assertEquals(1520.53, Reactancia.potenciaCondensador(220.0, 50.0, 100e-6), 1e-2)
    }

    @Test
    fun asociaciones() {
        assertEquals(60.0, Asociacion.suma(listOf(10.0, 20.0, 30.0)), 1e-12)
        assertEquals(5.0, Asociacion.inversa(listOf(10.0, 10.0)), 1e-12)
        assertThrows<ErrorCalculo> { Asociacion.suma(emptyList()) }
    }

    @Test
    fun circuitos() {
        assertEquals(5.0, Circuitos.divisorTension(12.0, 14.0, 10.0), 1e-12)
        assertEquals(0.4, Circuitos.divisorCorriente(1.0, 30.0, 20.0), 1e-12)
        assertEquals(150.0, Circuitos.resistenciaLed(5.0, 2.0, 0.02), 1e-9)
        assertEquals(50.0, Circuitos.resistenciaLed(12.0, 3.0, 0.06, 3), 1e-9)
        assertEquals(240.0, Circuitos.resistenciaZener(12.0, 5.1, 0.005, 0.02375), 1e-9)
        assertEquals(1200.0, Circuitos.joule(2.0, 10.0, 30.0), 1e-9)
        assertEquals(2.0, Circuitos.longitudOnda(Circuitos.VELOCIDAD_LUZ / 2), 1e-9)
        assertEquals(5.0, Circuitos.escalar(12.0, 4.0, 20.0, 0.0, 10.0), 1e-12)
        assertThrows<ErrorCalculo> { Circuitos.resistenciaSerie(5.0, 6.0, 0.01) }
    }
}
