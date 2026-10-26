package cl.electricalc.calc.conversiones

import cl.electricalc.calc.comun.ErrorCalculo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ConversionesTest {
    private fun u(lista: List<UnidadLineal>, s: String) = lista.first { it.simbolo == s }

    @Test
    fun lineales() {
        assertEquals(12.0, Unidades.convertir(1.0, u(Unidades.longitud, "ft"), u(Unidades.longitud, "in")), 1e-12)
        assertEquals(1.0, Unidades.convertir(745.69987158227022, u(Unidades.potencia, "W"), u(Unidades.potencia, "HP")), 1e-12)
        assertEquals(3.6, Unidades.convertir(1.0, u(Unidades.energia, "kWh"), u(Unidades.energia, "J")) / 1e6, 1e-12)
        assertEquals(14.5038, Unidades.convertir(1.0, u(Unidades.presion, "bar"), u(Unidades.presion, "psi")), 1e-4)
        assertEquals(10000.0, Unidades.convertir(1.0, u(Unidades.campoMagnetico, "T"), u(Unidades.campoMagnetico, "G")), 1e-9)
        assertEquals(1024.0, Unidades.convertir(1.0, u(Unidades.datos, "MiB"), u(Unidades.datos, "KiB")), 1e-12)
        assertEquals(180.0, Unidades.convertir(Math.PI, u(Unidades.angulo, "rad"), u(Unidades.angulo, "°")), 1e-12)
    }

    @Test
    fun especiales() {
        assertEquals(100.0, Temperatura.aCelsius(212.0, 1), 1e-12)
        assertEquals(-40.0, Temperatura.fahrenheit(-40.0), 1e-12)
        assertThrows<ErrorCalculo> { Temperatura.aCelsius(-1.0, 2) }
        assertEquals(311.127, Senoidal.picoDeEficaz(220.0), 1e-3)
        assertEquals(2.4, Conversiones.kwh(200.0, 12.0), 1e-12)
        assertEquals(200.0, Conversiones.ah(2.4, 12.0), 1e-12)
        val c = Conversiones.capacidadBanco(10000.0, 380.0, 50.0, triangulo = true)
        assertEquals(10000.0, Conversiones.reactivaBanco(c, 380.0, 50.0, triangulo = true), 1e-6)
        assertEquals(157.08, Conversiones.radPorSegundo(1500.0), 1e-2)
        val (ra, rb, rc) = Conversiones.trianguloAEstrella(30.0, 30.0, 30.0)
        assertEquals(10.0, ra, 1e-12); assertEquals(10.0, rb, 1e-12); assertEquals(10.0, rc, 1e-12)
        assertEquals(30.0, Conversiones.estrellaATriangulo(10.0, 10.0, 10.0).first, 1e-12)
    }

    @Test
    fun awg() {
        assertEquals(2.588, Awg.diametroMm(10), 1e-3)
        assertEquals(5.261, Awg.seccionMm2(10), 1e-3)
        assertEquals(10.0, Awg.calibre(Awg.seccionMm2(10)), 1e-9)
        assertEquals(107.2, Awg.seccionMm2(-3), 0.1)
        assertEquals("4/0 AWG", Awg.nombre(-3))
    }
}
