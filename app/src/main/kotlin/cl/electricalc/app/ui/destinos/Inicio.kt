package cl.electricalc.app.ui.destinos

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.datos.LocalPreferencias
import cl.electricalc.app.ui.catalogo.Catalogo
import cl.electricalc.app.ui.catalogo.Herramienta
import cl.electricalc.app.ui.componentes.FilaHerramienta
import cl.electricalc.app.ui.componentes.Titular

/** Acceso "¿Qué estás haciendo?": una lista curada de herramientas para un tipo de proyecto. */
class Acceso(val id: String, @StringRes val titulo: Int, val icono: ImageVector, val ids: List<String>)

object Accesos {
    val todos = listOf(
        Acceso(
            "vivienda", R.string.acceso_vivienda, Icons.Filled.Home,
            listOf(
                "cuadro_cargas", "seleccion_empalme", "circuitos_vivienda", "CalcoloCaduta", "verificacion_caida_ric",
                "TabCalcoloSezione", "selector_diferencial", "ImpiantoDiTerra", "volumenes_bano", "esq_unilineal_vivienda",
                "tabla_colores_ric", "memoria_calculo",
            ),
        ),
        Acceso(
            "comercial", R.string.acceso_comercial, Icons.Filled.Storefront,
            listOf(
                "cuadro_cargas", "seleccion_empalme", "tablero", "TabCalcoloSezione", "capacidad_ric", "CalcoloCaduta",
                "verificacion_caida_ric", "TabDimensionamentoDispositivoProtezione", "selector_diferencial", "iluminacion",
                "fp_recargo", "DispProtezioneEmergenza", "memoria_calculo",
            ),
        ),
        Acceso(
            "fotovoltaico", R.string.acceso_fotovoltaico, Icons.Filled.BatteryChargingFull,
            listOf("fv_netbilling", "esq_fv", "CalcoloCaduta", "TabCalcoloSezione", "NecessitaSpd", "DurataBatteria", "respaldo"),
        ),
        Acceso(
            "ve", R.string.acceso_ve, Icons.Filled.ElectricCar,
            listOf("carga_ve", "esq_ve", "seleccion_empalme", "selector_diferencial", "TabCalcoloSezione", "CalcoloCaduta"),
        ),
        Acceso(
            "motor", R.string.acceso_motor, Icons.Filled.Autorenew,
            listOf(
                "CorrenteMotore", "PotenzaMotore", "FattorePotenzaMotore", "RendimentoMotore", "CondensatoreAvviamentoMotore",
                "FullLoadCurrent", "TabCalcoloSezione", "CalcoloCaduta", "TabDimensionamentoDispositivoProtezione", "Rifasamento",
            ),
        ),
        Acceso(
            "tierra", R.string.acceso_tierra, Icons.Filled.Public,
            listOf(
                "ImpiantoDiTerra", "wenner", "electrodos_tierra", "verificacion_tt", "selector_diferencial",
                "esq_sistemas_tierra", "esq_puesta_tierra",
            ),
        ),
    )

    fun acceso(id: String) = todos.firstOrNull { it.id == id }
}

/** Inicio: buscador global, accesos por tipo de proyecto, recientes y favoritos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Inicio(
    catalogo: Catalogo,
    modifier: Modifier,
    alAbrir: (Herramienta) -> Unit,
    alMantener: (Herramienta) -> Unit,
    alAbrirPliego: (Int) -> Unit,
    alAbrirAcceso: (String) -> Unit,
) {
    val preferencias = LocalPreferencias.current
    var consulta by rememberSaveable { mutableStateOf("") }
    val resultados = remember(consulta) { catalogo.buscador.buscar(consulta) }

    LazyColumn(modifier) {
        item {
            OutlinedTextField(
                value = consulta,
                onValueChange = { consulta = it },
                placeholder = { Text(stringResource(R.string.buscar_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (consulta.isNotEmpty()) {
                    {
                        IconButton(onClick = { consulta = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.borrar_busqueda))
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }

        if (consulta.isNotBlank()) {
            if (resultados.vacio) {
                item { Text(stringResource(R.string.sin_resultados, consulta), modifier = Modifier.padding(16.dp)) }
            }
            if (resultados.pliegos.isNotEmpty()) item { Titular(stringResource(R.string.destino_ric)) }
            items(resultados.pliegos, key = { "p${it.numero}" }) { p ->
                ListItem(
                    headlineContent = { Text(p.titulo) },
                    overlineContent = { Text(p.codigo) },
                    leadingContent = { Icon(Icons.Filled.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { alAbrirPliego(p.numero) },
                )
            }
            if (resultados.herramientas.isNotEmpty()) {
                item { Titular(stringResource(R.string.herramientas_n, resultados.herramientas.size)) }
            }
            items(resultados.herramientas, key = { it.id }) { h ->
                FilaHerramienta(h, preferencias.esFavorito(h.id), alAbrir, alMantener)
            }
            return@LazyColumn
        }

        item {
            Titular(stringResource(R.string.que_estas_haciendo))
            FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Accesos.todos.forEach { a ->
                    ElevatedAssistChip(
                        onClick = { alAbrirAcceso(a.id) },
                        label = { Text(stringResource(a.titulo)) },
                        leadingIcon = { Icon(a.icono, contentDescription = null) },
                    )
                }
            }
        }

        val recientes = catalogo.ids(preferencias.recientes)
        if (recientes.isNotEmpty()) {
            item { Titular(stringResource(R.string.recientes)) }
            items(recientes, key = { "r" + it.id }) { FilaHerramienta(it, preferencias.esFavorito(it.id), alAbrir, alMantener) }
        }

        item { Titular(stringResource(R.string.favoritos)) }
        val favoritos = catalogo.favoritos(preferencias.favoritos)
        if (favoritos.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.sin_favoritos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        items(favoritos, key = { "f" + it.id }) { FilaHerramienta(it, true, alAbrir, alMantener) }
    }
}
