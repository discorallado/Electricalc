package cl.electricalc.app.ui.referencias

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.electricalc.app.ui.componentes.PantallaConBarra
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Una sección de una tabla de referencia: título, columnas, filas y notas, ya en el idioma actual. */
class SeccionReferencia(val titulo: String?, val columnas: List<String>, val filas: List<List<String>>, val notas: List<String>)

/**
 * Tablas de referencia (grados IP, códigos, pinouts…) guardadas en `assets/referencias/<id>.json`.
 * Cada texto es un literal o un objeto {"es": …, "en": …}.
 */
object Referencias {
    /** Ids del catálogo con tabla; `ReferenciasTest` comprueba que cada uno tenga su archivo. */
    val IDS = setOf("GradoProtezione", "CurveIntervento", "ClassiIsolamento", "SiPrefix", "UnitaMisura", "SigleFusibiliIec", "ClassiFusibiliNec", "TabAnsi", "MarcaturaAtex", "SistemiDistribuzione", "RisoluzioniCCTV")

    fun existe(id: String) = id in IDS

    fun ruta(id: String) = "referencias/$id.json"

    fun leer(json: String, idioma: String = Locale.getDefault().language): List<SeccionReferencia> {
        fun texto(v: Any?): String = when (v) {
            null, JSONObject.NULL -> ""
            is JSONObject -> v.optString(if (idioma == "es") "es" else "en", v.optString("es"))
            else -> v.toString()
        }
        fun lista(a: JSONArray?): List<String> = if (a == null) emptyList() else (0 until a.length()).map { texto(a.get(it)) }
        val secciones = JSONObject(json).getJSONArray("secciones")
        return (0 until secciones.length()).map { i ->
            val s = secciones.getJSONObject(i)
            val filas = s.optJSONArray("filas")
            SeccionReferencia(
                titulo = if (s.has("titulo")) texto(s.get("titulo")) else null,
                columnas = lista(s.optJSONArray("columnas")),
                filas = if (filas == null) emptyList() else (0 until filas.length()).map { lista(filas.getJSONArray(it)) },
                notas = lista(s.optJSONArray("notas")),
            )
        }
    }

    fun cargar(contexto: Context, id: String): List<SeccionReferencia> =
        leer(contexto.assets.open(ruta(id)).bufferedReader().use { it.readText() })
}

/** Filas de dos columnas se muestran con la segunda ajustada al ancho; con más, con desplazamiento lateral. */
@Composable
private fun TablaReferencia(s: SeccionReferencia) {
    val dos = s.columnas.size <= 2 && s.filas.all { it.size <= 2 }
    Column(if (dos) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState())) {
        @Composable
        fun fila(celdas: List<String>, cabecera: Boolean) {
            Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                celdas.forEachIndexed { i, c ->
                    val peso = if (cabecera) FontWeight.SemiBold else if (i == 0) FontWeight.Medium else FontWeight.Normal
                    val mod = when {
                        dos && i == 0 -> Modifier.width(88.dp)
                        dos -> Modifier.weight(1f)
                        else -> Modifier.width(if (i == 0) 96.dp else 140.dp)
                    }
                    Text(c, style = MaterialTheme.typography.bodyMedium, fontWeight = peso, modifier = mod)
                }
            }
        }
        if (s.columnas.isNotEmpty()) {
            fila(s.columnas, true)
            HorizontalDivider()
        }
        s.filas.forEach { fila(it, false) }
    }
}

@Composable
fun PantallaReferencia(titulo: String, id: String, alVolver: () -> Unit) {
    val contexto = LocalContext.current
    val secciones = remember(id) { Referencias.cargar(contexto, id) }
    PantallaConBarra(titulo, alVolver) { modificador ->
        Column(modificador.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            secciones.forEach { s ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.titulo?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                        if (s.filas.isNotEmpty()) TablaReferencia(s)
                        s.notas.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
