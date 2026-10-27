package cl.electricalc.app.ui.referencias

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ReferenciasTest {
    private fun json(id: String) = File("src/main/assets/${Referencias.ruta(id)}").readText()

    private val catalogo = JSONObject(File("../spec/catalogo-v2.json").readText())
        .getJSONArray("existentes").let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") }.toSet() }

    @Test
    fun `cada tabla tiene archivo, id del catalogo y filas del ancho de sus columnas`() {
        Referencias.IDS.forEach { id ->
            assertTrue(id in catalogo, "Id sin entrada en el catálogo: $id")
            listOf("es", "en").forEach { idioma ->
                val secciones = Referencias.leer(json(id), idioma)
                assertTrue(secciones.isNotEmpty(), id)
                secciones.forEach { s ->
                    s.filas.forEach { f ->
                        if (s.columnas.isNotEmpty()) assertEquals(s.columnas.size, f.size, "$id: ${s.titulo} $f")
                        assertTrue(f.none { it.isBlank() }, "$id ($idioma): celda vacía en ${s.titulo} $f")
                    }
                }
            }
        }
    }
}
