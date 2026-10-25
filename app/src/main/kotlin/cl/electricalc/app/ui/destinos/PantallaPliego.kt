package cl.electricalc.app.ui.destinos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.datos.LocalPreferencias
import cl.electricalc.app.ui.catalogo.Catalogo
import cl.electricalc.app.ui.catalogo.Herramienta
import cl.electricalc.app.ui.catalogo.Pliego
import cl.electricalc.app.ui.componentes.FilaHerramienta
import cl.electricalc.app.ui.componentes.PantallaConBarra
import cl.electricalc.app.ui.componentes.Titular

/** Sitio de la SEC. Se enlaza la portada: la ruta exacta de cada PDF se fija al descargarlos. */
private const val URL_SEC = "https://www.sec.cl"

/** Tarjeta gris para lo que todavía no está: tablas sin transcribir, checklist o esquemas. */
@Composable
fun AvisoPendiente(texto: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

/** Datos RIC que piden las herramientas de este pliego y que mencionan su número. */
private fun tablasPendientes(pliego: Pliego, herramientas: List<Herramienta>): List<String> {
    val marca = "N°%02d".format(pliego.numero)
    // En las tablas el dato es solo el pliego ("RIC N°03"): se antepone el nombre de la tabla.
    return herramientas.flatMap { h -> h.datosRic.map { if (it.startsWith("RIC")) "${h.titulo} ($it)" else it } }
        .filter { marca in it }
        .distinct()
}

/**
 * Página de un pliego: de qué trata (resumen propio, en borrador), herramientas, tablas y límites
 * (todavía sin transcribir), esquemas, checklist y el enlace a la SEC. No muestra valores normativos.
 */
@Composable
fun PantallaPliego(
    catalogo: Catalogo,
    pliego: Pliego,
    alVolver: () -> Unit,
    alAbrir: (Herramienta) -> Unit,
    alMantener: (Herramienta) -> Unit,
) {
    val preferencias = LocalPreferencias.current
    val contexto = LocalContext.current
    val resumen = stringArrayResource(R.array.ric_resumenes).getOrElse(pliego.numero - 1) { "" }
    val existentes = catalogo.ids(pliego.existentes)
    val nuevas = catalogo.ids(pliego.nuevas)
    val (esquemas, otrasNuevas) = nuevas.partition { it.tipo == "esquema" }
    val (checklists, herramientasNuevas) = otrasNuevas.partition { it.tipo == "checklist" }
    val herramientas = herramientasNuevas + existentes
    val porConfirmar = stringResource(R.string.relacion_por_confirmar)

    PantallaConBarra(pliego.codigo, alVolver) { modificador ->
        LazyColumn(modificador) {
            item {
                Text(
                    pliego.titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                Titular(stringResource(R.string.pliego_de_que_trata))
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.borrador)) })
                        }
                        Text(resumen, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.borrador_nota),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            item { Titular(stringResource(R.string.pliego_herramientas)) }
            if (herramientas.isEmpty()) item { AvisoPendiente(stringResource(R.string.pliego_sin_herramientas_texto)) }
            items(herramientas, key = { "h" + it.id }) { h ->
                FilaHerramienta(
                    h, preferencias.esFavorito(h.id), alAbrir, alMantener,
                    nota = if (!h.nueva && !h.ricSeguro) porConfirmar else null,
                )
            }

            item {
                Titular(stringResource(R.string.pliego_tablas))
                AvisoPendiente(stringResource(R.string.verificacion_ric_pendiente_texto))
            }
            items(tablasPendientes(pliego, herramientas + checklists)) { dato -> AvisoPendiente("• $dato") }

            item { Titular(stringResource(R.string.pliego_esquemas)) }
            if (esquemas.isEmpty()) item { AvisoPendiente(stringResource(R.string.pliego_sin_esquemas)) }
            items(esquemas, key = { "e" + it.id }) { FilaHerramienta(it, preferencias.esFavorito(it.id), alAbrir, alMantener) }

            item { Titular(stringResource(R.string.pliego_checklist)) }
            if (checklists.isEmpty()) item { AvisoPendiente(stringResource(R.string.pliego_sin_checklist)) }
            items(checklists, key = { "c" + it.id }) { FilaHerramienta(it, preferencias.esFavorito(it.id), alAbrir, alMantener) }

            item {
                Titular(stringResource(R.string.pliego_pdf))
                AvisoPendiente(stringResource(R.string.pliego_pdf_texto))
                OutlinedButton(
                    onClick = { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_SEC))) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Text(stringResource(R.string.abrir_sitio_sec), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
