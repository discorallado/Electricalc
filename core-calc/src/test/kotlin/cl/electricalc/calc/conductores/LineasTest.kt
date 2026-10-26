package cl.electricalc.calc.conductores

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.SistemaElectrico.MONOFASICA
import cl.electricalc.calc.comun.SistemaElectrico.TRIFASICA
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.sqrt

class LineasTest {
    @Test
    fun perdidasYTemperatura() {
        assertEquals(200.0, Lineas.perdidas(MONOFASICA, 10.0, 1.0), 1e-12)
        assertEquals(300.0, Lineas.perdidas(TRIFASICA, 10.0, 1.0), 1e-12)
        assertEquals(40.0, Lineas.temperatura(30.0, 70.0, 16.0, 32.0), 1e-12)
        assertThrows<ErrorCalculo> { Lineas.temperatura(70.0, 70.0, 1.0, 1.0) }
    }

    @Test
    fun corrientes() {
        assertEquals(10000 * 0.8 / (sqrt(3.0) * 380 * 0.9), Lineas.corrienteEmpleo(TRIFASICA, 10000.0, 380.0, 0.9, 0.8, 1.0), 1e-9)
        assertEquals(0.0, Lineas.corrienteNeutro(10.0, 10.0, 10.0), 1e-6)
        assertEquals(10.0, Lineas.corrienteNeutro(10.0, 0.0, 0.0), 1e-12)
        assertEquals(sqrt(300.0), Lineas.corrienteNeutro(20.0, 10.0, 0.0) * sqrt(300.0) / sqrt(300.0), 1e-9)
    }
}
