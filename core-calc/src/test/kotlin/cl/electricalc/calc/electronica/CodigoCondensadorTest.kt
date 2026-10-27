package cl.electricalc.calc.electronica

import cl.electricalc.calc.comun.ErrorCalculo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Compara con los vectores dorados generados con la clase original (`golden/condensadores.tsv`). */
class CodigoCondensadorTest {
    private fun normalizar(t: String?) = t?.replace(" ", "")?.replace(',', '.')?.replace('−', '-')

    @Test
    fun `coincide con la app de referencia`() {
        val lineas = javaClass.getResource("/golden/condensadores.tsv")!!.readText().lines().filter { it.isNotBlank() }
        lineas.forEach { l ->
            val (codigo, pf, tol, volt) = l.split("\t")
            if (pf == "ERROR") {
                assertThrows<ErrorCalculo>(codigo) { CodigoCondensador.leer(codigo) }
                return@forEach
            }
            val r = CodigoCondensador.leer(codigo)
            assertEquals(pf.toDouble(), r.pf, pf.toDouble() * 1e-12, codigo)
            assertEquals(tol.takeIf { it != "null" }, normalizar(r.tolerancia), codigo)
            assertEquals(volt.takeIf { it != "null" }?.toDouble(), r.tensionV, codigo)
        }
    }
}
