package cl.electricalc.calc.motores

import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.SistemaElectrico.CONTINUA
import cl.electricalc.calc.comun.SistemaElectrico.MONOFASICA
import cl.electricalc.calc.comun.SistemaElectrico.TRIFASICA
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.sqrt

class MotoresTest {
    @Test
    fun `corriente y sus inversas`() {
        val i = Motor.corriente(TRIFASICA, 7500.0, 380.0, 0.85, 0.9)
        assertEquals(7500 / (sqrt(3.0) * 380 * 0.85 * 0.9), i, 1e-9)
        assertEquals(7500.0, Motor.potencia(TRIFASICA, 380.0, i, 0.85, 0.9), 1e-6)
        assertEquals(380.0, Motor.tension(TRIFASICA, 7500.0, i, 0.85, 0.9), 1e-6)
        assertEquals(0.85, Motor.factorPotencia(TRIFASICA, 7500.0, 380.0, i, 0.9), 1e-9)
        assertEquals(0.9, Motor.rendimiento(TRIFASICA, 7500.0, 380.0, i, 0.85), 1e-9)
        assertEquals(1000 / (24 * 0.8), Motor.corriente(CONTINUA, 1000.0, 24.0, 0.5, 0.8), 1e-9)
        assertEquals(1000 / (220 * 0.8 * 0.7), Motor.corriente(MONOFASICA, 1000.0, 220.0, 0.8, 0.7), 1e-9)
        assertThrows<ErrorCalculo> { Motor.factorPotencia(TRIFASICA, 7500.0, 380.0, 1.0, 0.9) }
    }

    @Test
    fun `velocidad deslizamiento y par`() {
        assertEquals(1500.0, Motor.velocidadSincrona(50.0, 4), 1e-9)
        assertEquals(1440.0, Motor.velocidad(50.0, 4, 0.04), 1e-9)
        assertEquals(0.04, Motor.deslizamiento(50.0, 4, 1440.0), 1e-9)
        assertEquals(49.735, Motor.par(7500.0, 1440.0), 1e-3)
        assertEquals(7500.0, Motor.potenciaDePar(Motor.par(7500.0, 1440.0), 1440.0), 1e-9)
        assertThrows<ErrorCalculo> { Motor.velocidadSincrona(50.0, 3) }
    }
}
