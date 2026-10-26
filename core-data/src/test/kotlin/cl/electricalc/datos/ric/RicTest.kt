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
        assertEquals(28, Ric.todas.size)
        for (t in Ric.todas) {
            assertTrue(t.pliego in 1..19, t.titulo)
            assertTrue(t.version.startsWith("Resolución Exenta"), t.titulo)
            assertTrue(t.numeral.matches(Regex("""(Anexo )?\d+(\.\d+)*""")), "${t.titulo}: numeral ${t.numeral}")
            assertTrue(t.paginas.isNotEmpty() && t.paginas.all { it > 0 }, t.titulo)
            assertTrue(t.revision.estado in setOf("pendiente", "revisada"), t.titulo)
        }
        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 13, 15, 18, 19), Ric.todas.map { it.pliego }.toSet())
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

    @Test
    fun `puesta a tierra del RIC 06`() {
        val r = Ric.resistividad
        assertEquals(18, r.terrenos.size)
        assertTrue(r.terrenos.all { (it.minimo ?: 0.0) <= it.maximo }, "mínimo ≤ máximo")
        assertEquals(listOf(50.0, 500.0, 3000.0), r.valoresMedios.terrenos.map { it.valor })
        val e = Ric.electrodos
        // Pica de 2 m en 100 Ω·m: R = ρ/L = 50 Ω; placa de 4 m de perímetro: 0,8 × 100/4 = 20 Ω.
        assertEquals(50.0, e.resistencia("pica", 100.0, 2.0), 1e-9)
        assertEquals(20.0, e.resistencia("placa", 100.0, 4.0), 1e-9)
        assertEquals(10.0, e.resistencia("conductor", 100.0, 20.0), 1e-9)
        val p = Ric.seccionProteccion
        assertEquals(16.0, p.minima(16.0))
        assertEquals(25.0, p.minima(25.0))
        assertEquals(25.0, p.minima(35.0))
        assertEquals(35.0, p.minima(70.0))
        assertEquals(listOf("Schlumberger", "Wenner"), Ric.medicionResistividad.metodos)
    }

    @Test
    fun `motores RIC 07 y circuitos RIC 10`() {
        val m = Ric.motores
        assertEquals(12.5, m.corrienteDiseno(MotorGrupo(10.0)), 1e-9)
        assertEquals(1.4, m.factor("intermitente", "mas_60"))
        assertNull(m.factor("breve", "mas_60"))
        // Grupo permanente 10, 6, 4 A: 1,25·10 + 6 + 4 = 22,5 A.
        assertEquals(22.5, m.corrienteGrupo(listOf(MotorGrupo(10.0), MotorGrupo(6.0), MotorGrupo(4.0))), 1e-9)
        // Mixto: 10 A permanente + 8 A intermitente 15 min (0,85): 10 + 6,8 + 0,25·10 = 19,3 A.
        assertEquals(19.3, m.corrienteGrupo(listOf(MotorGrupo(10.0), MotorGrupo(8.0, "intermitente", "15"))), 1e-9)
        assertEquals(12.5, m.sobrecargaMaxima(10.0, 1.15), 1e-9)
        assertEquals(11.5, m.sobrecargaMaxima(10.0, 1.0), 1e-9)
        val c = Ric.circuitosAlumbrado
        assertEquals(30, c.diferencialMaxMa)
        // 10 A a 220 V, centros de 100 W: 0,9·10·220/100 = 19,8 → 19.
        assertEquals(19, c.centrosMaximos(10.0, 220.0, 100.0))
        assertEquals(5.5, c.corrienteRequerida(1100.0, 220.0), 1e-9)
        val v = Ric.vivienda
        assertEquals(2, v.circuitosMinimos(25.0))
        assertEquals(3, v.circuitosMinimos(30.0))
        assertEquals(2, v.centrosIluminacionMinimos(12.0))
        assertEquals(2, v.enchufesMinimos(14.0))
        assertEquals(1, v.enchufesMinimos(8.0))
    }

    @Test
    fun `empalmes RIC 01 y carga VE RIC 15`() {
        val e = Ric.empalmes
        assertEquals(11, e.monofasicos.size)
        assertEquals(18, e.trifasicos.size)
        assertTrue(e.trifasicos.zipWithNext().all { (a, b) -> b.a > a.a && b.kw > a.kw })
        assertEquals(25.0, e.para(4.5, trifasico = false)!!.a)
        assertEquals(25.0, e.para(1.0, trifasico = false, minimoA = 25.0)!!.a)
        assertEquals(50.0, e.para(25.0, trifasico = true)!!.a)
        val v = Ric.cargaVe
        assertEquals(1.25, v.factorProteccion(32.0))
        assertEquals(1.20, v.factorProteccion(100.0))
        assertEquals(1.15, v.factorProteccion(200.0))
        // Residencial 50 kW: 10 × 1 + 40 × (0,5 a 1) = 30 a 50 kW.
        assertEquals(30.0 to 50.0, v.demandaConSgc("residencial", 50.0))
    }

    @Test
    fun `ductos RIC 04`() {
        val d = Ric.ductos
        assertEquals(listOf(15, 19, 15, 20), d.tablas.map { it.filas.size })
        for (t in d.tablas) for (f in t.filas) {
            assertEquals(5, f.ductosMm.size, "${t.tabla} ${f.seccion}")
            assertTrue(f.ductosMm.zipWithNext().all { (a, b) -> b >= a }, "${t.tabla} ${f.seccion}: el ducto no baja con más conductores")
        }
        for (t in d.tablas) assertTrue(t.filas.zipWithNext().all { (a, b) -> b.areaMm2 > a.areaMm2 }, t.tabla)
        // Tubería de 20 mm interior (314 mm²) y cable de 11 mm²: 33 % → 9,4 → 9 conductores.
        assertEquals(9, d.cantidadMaxima(20.0, 11.0))
        // 0,86 de un segundo conductor se aproxima, pero no alcanza para 2: queda 1.
        assertEquals(1, d.cantidadMaxima(10.0, 30.0))
        assertEquals(4, d.cantidadEnBandeja(1000.0, 100.0))
    }

    @Test
    fun `diferenciales RIC 05 y 15`() {
        val c = Ric.contactos
        assertEquals(1666.67, c.resistenciaMaximaTt(0.03, humedo = false), 0.01)
        assertEquals(800.0, c.resistenciaMaximaTt(0.03, humedo = true), 1e-9)
        assertEquals(30, c.tt.diferencialMaxMa)
        assertEquals("B", c.clases.sinPasoPorCero)
        assertEquals("B", Ric.diferencialesVe.modo(3).tipo)
        assertEquals(30, Ric.diferencialesVe.modo(1).sensibilidadMa)
    }

    @Test
    fun `autogeneracion y tablero`() {
        val a = Ric.autogeneracion
        val casa = a.para(3.0, 10.0, conInversor = true, inyeccionCero = true)
        assertEquals(30, casa.diferencialMa)
        assertEquals(false, casa.potenciaInversa)
        assertEquals(false, casa.riCentralizada)
        val media = a.para(50.0, 300.0, conInversor = true, inyeccionCero = true)
        assertEquals(true, media.potenciaInversa)
        assertEquals(10.0, media.tiempoInversaS)
        assertEquals(true, media.registrador)
        val grande = a.para(2000.0, 5000.0, conInversor = true, inyeccionCero = false)
        assertEquals(2.0, grande.tiempoInversaS)
        assertEquals(true, grande.riCentralizada)
        assertEquals(300, grande.diferencialMa)
        assertEquals(30, Ric.tableros.modulosConReserva(24))
        assertEquals(1.2, Ric.tableros.espaciosTrabajo[1].m)
    }

    @Test
    fun `volumenes de bano`() {
        val v = Ric.volumenesBano
        assertEquals(1, v.volumen(TipoDucha.CON_RECEPTACULO, 0.0, 1.5))
        assertEquals(2, v.volumen(TipoDucha.CON_RECEPTACULO, 0.5, 1.0))
        assertEquals(3, v.volumen(TipoDucha.CON_RECEPTACULO, 2.0, 1.0))
        assertEquals(null, v.volumen(TipoDucha.CON_RECEPTACULO, 3.5, 1.0))
        assertEquals(0, v.volumen(TipoDucha.DIFUSOR_MOVIL, 1.0, 0.03))
        assertEquals(2, v.volumen(TipoDucha.DIFUSOR_FIJO, 0.5, 2.5, techo = 2.6))
        assertEquals(null, v.volumen(TipoDucha.DIFUSOR_FIJO, 0.5, 2.7, techo = 2.6))
        assertEquals("IPX7", v.requisitos(0).ip)
    }

    @Test
    fun `impedancia de bucle`() {
        val b = Ric.impedanciaBucle
        assertEquals(1.4, b.maxima(16, 'C'))
        assertEquals(0.17, b.maxima(63, 'D'))
        assertEquals(EstadoVerificacion.CUMPLE, VerificacionRic.bucle(1.2, false, 16, 'C').estado)
        assertEquals(EstadoVerificacion.NO_CUMPLE, VerificacionRic.bucle(85.0, true, 16, 'C').estado)
        assertEquals(233.2, b.ipcc(1.0), 1e-9)
    }

    @Test
    fun transformadores() {
        val t = Ric.transformadores
        assertEquals(6.0, t.tabla133(0.04).primarioAutomatico)
        assertEquals(1.25, t.tabla133(0.07).secundarioMtFusible)
        assertEquals(0.1, t.ventilacionM2(30.0))
        assertEquals(1.0, t.ventilacionM2(500.0), 1e-9)
        assertEquals(true, t.vigilanciaTermica(150.0, aceite = false))
        assertEquals(false, t.vigilanciaTermica(150.0, aceite = true))
    }
}
