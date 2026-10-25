package cl.electricalc.app.ui.componentes

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.ui.calculadoras.Registro
import cl.electricalc.app.ui.catalogo.Herramienta
import cl.electricalc.app.ui.catalogo.IconoItem
import cl.electricalc.app.ui.formulario.Fichas

@StringRes
fun nombreCategoria(id: String): Int = when (id) {
    "fundamentos" -> R.string.cat_fundamentos
    "conductores" -> R.string.cat_conductores
    "protecciones" -> R.string.cat_protecciones
    "tierra" -> R.string.cat_tierra
    "energia" -> R.string.cat_energia
    "motores" -> R.string.cat_motores
    "sistemas" -> R.string.cat_sistemas
    "electronica" -> R.string.cat_electronica
    "ref_tablas" -> R.string.cat_ref_tablas
    "ref_esquemas" -> R.string.cat_ref_esquemas
    "ref_conectores" -> R.string.cat_ref_conectores
    "ref_unidades" -> R.string.cat_ref_unidades
    else -> R.string.cat_ref_formulario
}

@StringRes
fun nombreEtapa(id: String): Int = when (id) {
    "cargas" -> R.string.etapa_cargas
    "empalme" -> R.string.etapa_empalme
    "tableros" -> R.string.etapa_tableros
    "alimentadores" -> R.string.etapa_alimentadores
    "protecciones" -> R.string.etapa_protecciones
    "tierra" -> R.string.etapa_tierra
    "sistemas" -> R.string.etapa_sistemas
    else -> R.string.etapa_documentacion
}

@StringRes
fun nombreTipo(tipo: String?): Int = when (tipo) {
    "verificacion" -> R.string.tipo_verificacion
    "asistente" -> R.string.tipo_asistente
    "esquema" -> R.string.tipo_esquema
    "documento" -> R.string.tipo_documento
    "checklist" -> R.string.tipo_checklist
    "tabla" -> R.string.tipo_tabla
    else -> R.string.tipo_calculadora
}

/** Si la herramienta ya se puede abrir (calculadora o ficha implementada). */
fun disponible(h: Herramienta) = Registro.implementada(h.id) || Fichas.existe(h.id)

/**
 * Fila de una herramienta: ícono, título, estado y estrella si es favorita. Tocar la abre y
 * mantener pulsado la agrega o quita de Favoritos.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilaHerramienta(
    h: Herramienta,
    favorita: Boolean,
    alAbrir: (Herramienta) -> Unit,
    alMantener: (Herramienta) -> Unit,
    modifier: Modifier = Modifier,
    /** Texto extra bajo el título (p. ej. "relación por confirmar"). */
    nota: String? = null,
) {
    val estado = listOfNotNull(
        if (h.esFicha) stringResource(R.string.cat_ref_formulario) else null,
        when {
            disponible(h) -> null
            h.nueva -> stringResource(R.string.nueva_proximamente, stringResource(nombreTipo(h.tipo)))
            else -> stringResource(R.string.proximamente)
        },
        nota,
    ).joinToString(" · ").ifEmpty { null }
    ListItem(
        headlineContent = { Text(h.titulo) },
        supportingContent = estado?.let { { Text(it, color = MaterialTheme.colorScheme.outline) } },
        leadingContent = { IconoItem(h) },
        trailingContent = if (favorita) {
            { Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.favorito), tint = MaterialTheme.colorScheme.primary) }
        } else {
            null
        },
        modifier = modifier.combinedClickable(onClick = { alAbrir(h) }, onLongClick = { alMantener(h) }),
    )
    HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

/** Lista de filas con un título de sección opcional. */
@Composable
fun BloqueHerramientas(
    titulo: String?,
    items: List<Herramienta>,
    favoritos: Set<String>,
    alAbrir: (Herramienta) -> Unit,
    alMantener: (Herramienta) -> Unit,
) {
    Column {
        if (titulo != null) Titular(titulo)
        items.forEach { FilaHerramienta(it, it.id in favoritos, alAbrir, alMantener) }
    }
}

@Composable
fun Titular(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/**
 * Un chip "RIC N°xx" por pliego relacionado; abre la página del pliego. Si la relación tiene
 * confianza media (hay que confirmar el numeral en el PDF) el ícono es un signo de pregunta.
 */
@Composable
fun ChipsRic(h: Herramienta, alAbrirPliego: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (h.ric.isEmpty()) return
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        h.ric.forEach { n ->
            AssistChip(
                onClick = { alAbrirPliego(n) },
                label = { Text("RIC N°%02d".format(n)) },
                leadingIcon = {
                    Icon(
                        if (h.ricSeguro) Icons.Filled.Gavel else Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = if (h.ricSeguro) null else stringResource(R.string.relacion_por_confirmar),
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}
