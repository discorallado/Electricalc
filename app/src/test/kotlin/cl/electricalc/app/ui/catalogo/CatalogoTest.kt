package cl.electricalc.app.ui.catalogo

import cl.electricalc.app.ui.calculadoras.Registro
import cl.electricalc.app.ui.destinos.Accesos
import cl.electricalc.app.ui.formulario.Fichas
import cl.electricalc.app.ui.ric.TablasRic
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Locale

/** Carga los JSON de `spec/` tal como los copia Gradle a los assets y prueba el catálogo y el buscador. */
class CatalogoTest {

    companion object {
        private fun spec(nombre: String) = File("../spec/$nombre").readText()

        lateinit var catalogo: Catalogo

        @JvmStatic
        @BeforeAll
        fun cargar() {
            Locale.setDefault(Locale.forLanguageTag("es-CL"))
            catalogo = Catalogo.desdeJson(spec("catalogo-v2.json"), spec("catalogo.json"), spec("sinonimos.json"))
        }
    }

    private fun ids(consulta: String) = catalogo.buscador.buscar(consulta).herramientas.map { it.id }

    @Test
    fun `el catalogo trae todas las herramientas y los 19 pliegos`() {
        assertEquals(221, catalogo.herramientas.count { !it.nueva })
        assertEquals(37, catalogo.herramientas.count { it.nueva })
        assertEquals((1..19).toList(), catalogo.pliegos.map { it.numero })
        assertEquals("Cálculo de la corriente", catalogo.item("CalcoloIntensita")?.tituloEs)
        assertEquals("Calculation of current", catalogo.item("CalcoloIntensita")?.tituloEn)
        assertEquals("ico_intensita", catalogo.item("CalcoloIntensita")?.icono)
    }

    @Test
    fun `cada categoria del catalogo tiene su lugar en Calcular o Referencias`() {
        val categorias = catalogo.herramientas.map { it.categoria }.toSet()
        assertEquals(categorias, (Categorias.CALCULAR + Categorias.REFERENCIAS).toSet())
        val etapas = catalogo.herramientas.mapNotNull { it.etapa }.toSet()
        assertEquals(etapas, ETAPAS.toSet())
    }

    @Test
    fun `calculadoras fichas accesos y sinonimos apuntan a ids que existen`() {
        val calculadoras = Registro.todas
        calculadoras.forEach { assertNotNull(catalogo.item(it.id), "Calculadora sin entrada en el catálogo: ${it.id}") }
        listOf(
            "FormulaCorrente", "FormulaTensione", "FormulaPotenzaAttiva", "FormulaPotenzaApparente",
            "FormulaPotenzaReattiva", "FormulaFattorePotenza", "FormulaResistenza", "FormulaImpedenza", "FormulaCaduta",
            "FormulaCadutaCarichiDistribuiti", "FormulaLunghezzaCavoCaduta",
        ).forEach {
            assertTrue(Fichas.existe(it), "Ficha no registrada: $it")
            assertNotNull(catalogo.item(it), "Ficha sin entrada en el catálogo: $it")
        }
        calculadoras.forEach { c ->
            // Las herramientas nuevas no traen ficha en el catálogo; si la declaran, debe existir.
            val formula = catalogo.item(c.id)!!.formula ?: return@forEach
            assertTrue(Fichas.existe(formula), "La calculadora ${c.id} apunta a una ficha sin implementar: $formula")
        }
        listOf("tabla_caidas_admisibles", "tabla_secciones_minimas", "tabla_colores_ric").forEach {
            assertNotNull(catalogo.item(it), "Tabla RIC sin entrada en el catálogo: $it")
            assertNotNull(TablasRic.claveDeHerramienta(it), it)
        }
        Accesos.todos.flatMap { it.ids }.forEach { assertNotNull(catalogo.item(it), "Acceso con id inexistente: $it") }
        JSONObject(spec("sinonimos.json")).keys().asSequence().filterNot { it.startsWith("_") }.forEach {
            assertNotNull(catalogo.item(it), "Sinónimos de un id inexistente: $it")
        }
    }

    @Test
    fun `buscar RIC 4 lleva al pliego y a sus herramientas`() {
        for (consulta in listOf("RIC 4", "ric n°04", "RIC Nº4", "ric4")) {
            val r = catalogo.buscador.buscar(consulta)
            assertEquals(listOf(4), r.pliegos.map { it.numero }, consulta)
            assertTrue(r.herramientas.isNotEmpty(), consulta)
            assertTrue(r.herramientas.all { 4 in it.ric }, consulta)
        }
        // El resto del texto filtra dentro del pliego.
        val r = catalogo.buscador.buscar("RIC 3 caída")
        assertTrue(r.herramientas.isNotEmpty() && r.herramientas.all { 3 in it.ric })
        assertTrue("CalcoloCaduta" in r.herramientas.map { it.id })
    }

    @Test
    fun `buscar no distingue tildes ni mayusculas y usa los sinonimos`() {
        assertEquals(ids("caída"), ids("CAIDA"))
        assertEquals("CalcoloCaduta", ids("caida de tension").first())
        assertTrue("selector_diferencial" in ids("diferencial"))
        assertTrue("ImpiantoDiTerra" in ids("diferencial"))
        assertTrue("selector_diferencial" in ids("RCD"), "por sinónimo")
        assertTrue("fv_netbilling" in ids("paneles solares"), "por sinónimo")
        assertTrue("TabCalcoloSezione" in ids("calibre"), "por sinónimo")
        assertEquals(listOf(6), catalogo.buscador.buscar("puesta a tierra").pliegos.map { it.numero })
        assertTrue(catalogo.buscador.buscar("zzzz").vacio)
        assertTrue(catalogo.buscador.buscar("  ").vacio)
    }

    @Test
    fun `la ficha va despues de su calculadora`() {
        val r = ids("corriente")
        assertTrue(r.indexOf("CalcoloIntensita") < r.indexOf("FormulaCorrente"))
    }

    @Test
    fun `las categorias ponen primero las herramientas con verificacion RIC`() {
        val conductores = catalogo.categoria("conductores")
        val primeraSin = conductores.indexOfFirst { it.datosRic.isEmpty() }
        assertTrue(conductores.drop(primeraSin).all { it.datosRic.isEmpty() })
        assertTrue(conductores.first().datosRic.isNotEmpty())
    }
}
