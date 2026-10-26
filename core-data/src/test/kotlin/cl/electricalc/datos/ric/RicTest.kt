package cl.electricalc.datos.ric

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Forma y coherencia de las tablas transcritas. No reemplaza la revisión contra el PDF: verifica
 * que cada archivo cargue, cite su fuente y no tenga valores imposibles.
 */
class RicTest {

    @Test
    fun `todas las tablas cargan y citan su fuente`() {
        assertEquals(9, Ric.todas.size)
        for (t in Ric.todas) {
            assertTrue(t.pliego in 1..19, t.titulo)
            assertTrue(t.version.startsWith("Resolución Exenta"), t.titulo)
            assertTrue(t.numeral.matches(Regex("""\d+(\.\d+)+""")), "${t.titulo}: numeral ${t.numeral}")
            assertTrue(t.paginas.isNotEmpty() && t.paginas.all { it > 0 }, t.titulo)
            assertTrue(t.revision.estado in setOf("pendiente", "revisada"), t.titulo)
        }
        assertEquals(setOf(3, 4), Ric.todas.map { it.pliego }.toSet())
        assertEquals(3, Ric.delPliego(3).size)
    }

    @Test
    fun `caidas admisibles del RIC 03`() {
        assertEquals(3.0, Ric.caidas.porTramo)
        assertEquals(5.0, Ric.caidas.total)
        assertEquals("5.1.3", Ric.caidas.numeral)
    }

    @Test
    fun `factores de demanda por tramos`() {
        val t = Ric.factoresDemanda
        assertEquals(7, t.consumidores.size)
        // Casa habitación, 10 kW: 3 × 1 + 7 × 0,35 = 5,45 kW.
        assertEquals(5.45, t.demanda("Casa habitación", 10.0), 1e-9)
        // 200 kW: 3 + 117 × 0,35 + 80 × 0,25 = 63,95 kW.
        assertEquals(63.95, t.demanda("Casa habitación", 200.0), 1e-9)
        assertEquals(30.0, t.demanda("Todo otro tipo", 30.0), 1e-9)
        for (c in t.consumidores) {
            assertNull(c.tramos.last().hastaKw, "${c.tipo}: el último tramo no tiene límite")
            assertTrue(c.tramos.all { it.factor in 0.0..1.0 }, c.tipo)
        }
    }

    @Test
    fun `factores de simultaneidad continuan en la expresion sobre 21`() {
        val t = Ric.simultaneidad
        assertEquals(21, t.porCantidad.size)
        assertEquals(1.0, t.factor(1))
        assertEquals(0.95, t.factor(4))
        assertEquals(0.73, t.factor(21))
        // La tabla y la expresión empalman en n = 21: (15,3 + 0)/21 = 0,7286.
        assertEquals(15.3 / 21, t.factor(21), 0.005)
        assertEquals((15.3 + 0.5) / 22, t.factor(22), 1e-12)
        assertTrue(t.porCantidad.zipWithNext().all { (a, b) -> b <= a }, "Los factores no deben subir con n")
    }

    @Test
    fun `secciones minimas y colores`() {
        val s = Ric.seccionesMinimas
        assertEquals(1.5, s.minima("iluminacion")?.mm2)
        assertEquals(4.0, s.minima("alimentador")?.mm2)
        assertNull(s.minima("otro"))
        assertEquals(listOf("azul", "negro", "rojo"), Ric.colores.conductores.take(3).map { it.color })
    }

    @Test
    fun `tabla 4_4 completa y sin bajas al aumentar la seccion`() {
        val t = Ric.capacidad
        assertEquals(listOf(70, 90), t.bloques.map { it.temperaturaServicio })
        for (b in t.bloques) {
            assertEquals(34, b.filas.size)
            assertEquals(1.5, b.filas.first().mm2)
            assertEquals(300.0, b.filas.last().mm2)
            assertTrue(b.filas.zipWithNext().all { (a, c) -> c.mm2 > a.mm2 })
            assertTrue(b.metodos.all { it in t.metodos.keys })
            for (m in b.metodos) {
                val columna = b.filas.mapNotNull { it.amperes[m] }
                assertTrue(columna.zipWithNext().all { (a, c) -> c >= a }, "${b.temperaturaServicio} °C, $m")
            }
        }
        assertEquals(14, t.capacidad(70, "A1", 1.5))
        assertEquals(497, t.capacidad(70, "E", 300.0))
        assertEquals(736, t.capacidad(90, "F", 300.0))
        assertNull(t.capacidad(90, "F", 2.5), "El método F no tiene valor bajo 4 mm²")
        assertEquals(setOf("D1", "D2"), t.bloque(90)!!.temperaturaAmbiente.filterValues { it == 20 }.keys)
    }

    @Test
    fun `tabla 4_5 coincide con el metodo E a 70 grados desde 25 mm2`() {
        val movil = Ric.capacidadMovil.filas
        assertEquals(37, movil.size)
        assertEquals(18.5, movil.first { it.mm2 == 1.5 }.amperes)
        val e70 = Ric.capacidad.bloque(70)!!.filas.filter { it.mm2 >= 25 }
        for (f in e70) assertEquals(f.amperes["E"]!!.toDouble(), movil.first { it.mm2 == f.mm2 }.amperes, "${f.mm2} mm²")
    }

    @Test
    fun `factores de correccion fn y ft`() {
        assertEquals(1.0, Ric.fn.fn(3))
        assertEquals(0.8, Ric.fn.fn(4))
        assertEquals(0.7, Ric.fn.fn(24))
        assertEquals(0.5, Ric.fn.fn(100))
        val ft = Ric.ft
        assertEquals(1.0, ft.ft("70C", 30))
        assertEquals(1.0, ft.ft("90C", 28))
        // Los métodos enterrados usan 20 °C de referencia: en 16–20 °C el factor es 1.
        assertEquals(1.0, ft.ft("90C_D", 20))
        assertEquals(0.5, ft.ft("70C", 60))
        assertNull(ft.ft("70C", 61))
        assertEquals("90C_D", ft.columna(90, "D2")?.id)
        assertEquals("70C", ft.columna(70, "B1")?.id)
        for (columna in ft.columnas.map { it.id }) {
            val valores = ft.filas.map { it.factores.getValue(columna) }
            assertTrue(valores.zipWithNext().all { (a, b) -> b < a }, "ft debe bajar con la temperatura ($columna)")
        }
    }

    @Test
    fun `verificacion de caida y seccion`() {
        val ok = VerificacionRic.caida(2.8)
        assertEquals(listOf(EstadoVerificacion.CUMPLE, EstadoVerificacion.CUMPLE), ok.map { it.estado })
        val tramo = VerificacionRic.caida(3.5)
        assertEquals(EstadoVerificacion.NO_CUMPLE, tramo.first { it.tipo == TipoVerificacion.CAIDA_TRAMO }.estado)
        assertEquals(EstadoVerificacion.CUMPLE, tramo.first { it.tipo == TipoVerificacion.CAIDA_TOTAL }.estado)
        assertEquals(EstadoVerificacion.CUMPLE, VerificacionRic.caida(3.0).first().estado, "El límite es inclusivo")

        assertEquals(EstadoVerificacion.REVISAR, VerificacionRic.seccion(2.5, null).estado)
        assertEquals(EstadoVerificacion.CUMPLE, VerificacionRic.seccion(2.5, "enchufes").estado)
        assertEquals(EstadoVerificacion.NO_CUMPLE, VerificacionRic.seccion(1.5, "enchufes").estado)
        assertEquals(EstadoVerificacion.NO_CUMPLE, VerificacionRic.seccion(2.5, "alimentador").estado)
        val v = VerificacionRic.seccion(4.0, "alimentador")
        assertNotNull(v.uso)
        assertEquals("5.4", v.fuente.numeral)
    }
}
