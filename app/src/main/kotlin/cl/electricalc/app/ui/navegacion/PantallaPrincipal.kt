package cl.electricalc.app.ui.navegacion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.datos.LocalPreferencias
import cl.electricalc.app.ui.catalogo.Catalogo
import cl.electricalc.app.ui.catalogo.Categorias
import cl.electricalc.app.ui.catalogo.Herramienta
import cl.electricalc.app.ui.destinos.ListaCategorias
import cl.electricalc.app.ui.destinos.Inicio
import cl.electricalc.app.ui.destinos.ListaPliegos
import cl.electricalc.app.ui.destinos.Proyecto
import kotlinx.coroutines.launch

/** Destinos de la barra inferior (`docs/arquitectura-v2.md`, sección 2). */
enum class Destino(val titulo: Int, val icono: ImageVector) {
    INICIO(R.string.destino_inicio, Icons.Filled.Home),
    PROYECTO(R.string.destino_proyecto, Icons.AutoMirrored.Filled.Assignment),
    CALCULAR(R.string.destino_calcular, Icons.Filled.Calculate),
    RIC(R.string.destino_ric, Icons.Filled.Gavel),
    REFERENCIAS(R.string.destino_referencias, Icons.AutoMirrored.Filled.MenuBook),
}

/** Acciones de navegación que comparten todos los destinos. */
class Acciones(
    val abrir: (Herramienta) -> Unit,
    val abrirPliego: (Int) -> Unit,
    val abrirCategoria: (String) -> Unit,
    val abrirAcceso: (String) -> Unit,
    val abrirAjustes: () -> Unit,
    val abrirAcerca: () -> Unit,
    val abrirGuiaUso: () -> Unit,
    val abrirProyecto: (String) -> Unit,
    val nuevoProyecto: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(catalogo: Catalogo, acciones: Acciones) {
    val preferencias = LocalPreferencias.current
    val contexto = LocalContext.current
    val cajon = rememberDrawerState(DrawerValue.Closed)
    val alcance = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var destino by rememberSaveable { mutableIntStateOf(0) }
    val actual = Destino.entries[destino]

    // Atrás desde otro destino vuelve a Inicio antes de salir de la app.
    BackHandler(enabled = actual != Destino.INICIO) { destino = 0 }

    val alMantener: (Herramienta) -> Unit = { h ->
        val agregado = !preferencias.esFavorito(h.id)
        preferencias.alternarFavorito(h.id)
        alcance.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(
                contexto.getString(if (agregado) R.string.favorito_agregado else R.string.favorito_quitado, h.titulo),
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = cajon,
        drawerContent = {
            ModalDrawerSheet {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(24.dp))
                HorizontalDivider()
                listOf(
                    Triple(R.string.ajustes, Icons.Outlined.Settings, acciones.abrirAjustes),
                    Triple(R.string.guia_uso, Icons.AutoMirrored.Outlined.HelpOutline, acciones.abrirGuiaUso),
                    Triple(R.string.acerca_de, Icons.Outlined.Info, acciones.abrirAcerca),
                ).forEach { (texto, icono, accion) ->
                    NavigationDrawerItem(
                        label = { Text(stringResource(texto)) },
                        icon = { Icon(icono, contentDescription = null) },
                        selected = false,
                        onClick = {
                            alcance.launch { cajon.close() }
                            accion()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (actual == Destino.INICIO) stringResource(R.string.app_name) else stringResource(actual.titulo)) },
                    navigationIcon = {
                        IconButton(onClick = { alcance.launch { cajon.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu))
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    Destino.entries.forEachIndexed { i, d ->
                        NavigationBarItem(
                            selected = i == destino,
                            onClick = { destino = i },
                            icon = { Icon(d.icono, contentDescription = null) },
                            label = { Text(stringResource(d.titulo), maxLines = 1) },
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { relleno ->
            val m = Modifier.padding(relleno).consumeWindowInsets(relleno).imePadding()
            when (actual) {
                Destino.INICIO -> Inicio(catalogo, m, acciones.abrir, alMantener, acciones.abrirPliego, acciones.abrirAcceso)
                Destino.PROYECTO -> Proyecto(catalogo, m, acciones.abrir, alMantener, acciones.abrirProyecto, acciones.nuevoProyecto)
                Destino.CALCULAR -> ListaCategorias(catalogo, Categorias.CALCULAR, m, acciones.abrirCategoria)
                Destino.RIC -> ListaPliegos(catalogo, m, acciones.abrirPliego)
                Destino.REFERENCIAS -> ListaCategorias(catalogo, Categorias.REFERENCIAS, m, acciones.abrirCategoria)
            }
        }
    }
}
