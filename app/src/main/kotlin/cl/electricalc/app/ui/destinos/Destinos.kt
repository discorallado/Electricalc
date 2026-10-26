package cl.electricalc.app.ui.destinos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.datos.LocalPreferencias
import cl.electricalc.app.ui.catalogo.Catalogo
import cl.electricalc.app.ui.catalogo.ETAPAS
import cl.electricalc.app.ui.catalogo.Herramienta
import cl.electricalc.app.ui.catalogo.iconoCategoria
import cl.electricalc.app.ui.componentes.FilaHerramienta
import cl.electricalc.app.ui.componentes.Titular
import cl.electricalc.app.ui.componentes.disponible
import cl.electricalc.app.ui.componentes.nombreCategoria
import cl.electricalc.app.ui.componentes.nombreEtapa

/** Círculo con un número o código corto (etapas, pliegos). */
@Composable
fun Insignia(texto: String) {
    Box(
        Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/**
 * Proyecto: el flujo de 8 etapas con sus herramientas en orden. Guardar proyectos y pasar los
 * resultados de una etapa a la siguiente llega en la fase C.
 */
@Composable
fun Proyecto(
    catalogo: Catalogo,
    modifier: Modifier,
    alAbrir: (Herramienta) -> Unit,
    alMantener: (Herramienta) -> Unit,
    alAbrirProyecto: (String) -> Unit,
    alNuevoProyecto: () -> Unit,
) {
    val preferencias = LocalPreferencias.current
    LazyColumn(modifier) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.proyecto_titulo), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.proyecto_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Button(onClick = alNuevoProyecto, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.nuevo_proyecto))
                    }
                }
            }
        }
        if (preferencias.proyectos.isNotEmpty()) item { Titular(stringResource(R.string.mis_proyectos)) }
        items(preferencias.proyectos, key = { "p" + it.id }) { pr ->
            ListItem(
                headlineContent = { Text(pr.nombre) },
                supportingContent = { Text(pluralStringResource(R.plurals.cargas_n, pr.cargas.size, pr.cargas.size)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable { alAbrirProyecto(pr.id) },
            )
        }
        item { Titular(stringResource(R.string.herramientas_por_etapa)) }
        ETAPAS.forEachIndexed { i, etapa ->
            item(key = etapa) {
                ListItem(
                    headlineContent = { Text(stringResource(nombreEtapa(etapa)), fontWeight = FontWeight.SemiBold) },
                    leadingContent = { Insignia("${i + 1}") },
                )
            }
            items(catalogo.etapa(etapa), key = { "$etapa/${it.id}" }) { h ->
                FilaHerramienta(h, preferencias.esFavorito(h.id), alAbrir, alMantener, Modifier.padding(start = 24.dp))
            }
        }
    }
}

/** Calcular o Referencias: una fila por categoría con cuántas herramientas hay disponibles. */
@Composable
fun ListaCategorias(catalogo: Catalogo, categorias: List<String>, modifier: Modifier, alAbrirCategoria: (String) -> Unit) {
    LazyColumn(modifier) {
        items(categorias, key = { it }) { id ->
            val items = catalogo.categoria(id)
            ListItem(
                headlineContent = { Text(stringResource(nombreCategoria(id))) },
                supportingContent = {
                    Text(pluralStringResource(R.plurals.disponibles_de, items.size, items.count(::disponible), items.size))
                },
                leadingContent = {
                    Box(
                        Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(iconoCategoria(id), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                modifier = Modifier.clickable { alAbrirCategoria(id) },
            )
            HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** RIC: los 19 pliegos con su número, título y cuántas herramientas tienen. */
@Composable
fun ListaPliegos(catalogo: Catalogo, modifier: Modifier, alAbrirPliego: (Int) -> Unit) {
    LazyColumn(modifier) {
        item {
            Text(
                stringResource(R.string.ric_intro),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
        items(catalogo.pliegos, key = { it.numero }) { p ->
            val total = p.existentes.size + p.nuevas.size
            ListItem(
                headlineContent = { Text(p.titulo) },
                overlineContent = { Text(p.codigo) },
                supportingContent = {
                    Text(
                        if (total == 0) {
                            stringResource(R.string.pliego_sin_herramientas)
                        } else {
                            pluralStringResource(R.plurals.herramientas, total, total)
                        },
                    )
                },
                leadingContent = { Insignia("%02d".format(p.numero)) },
                modifier = Modifier.clickable { alAbrirPliego(p.numero) },
            )
            HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Lista simple (una categoría o un acceso de Inicio). */
@Composable
fun ListaHerramientas(
    items: List<Herramienta>,
    modifier: Modifier,
    alAbrir: (Herramienta) -> Unit,
    alMantener: (Herramienta) -> Unit,
    encabezado: String? = null,
) {
    val preferencias = LocalPreferencias.current
    LazyColumn(modifier) {
        if (encabezado != null) item { Titular(encabezado) }
        items(items, key = { it.id }) { FilaHerramienta(it, preferencias.esFavorito(it.id), alAbrir, alMantener) }
    }
}
