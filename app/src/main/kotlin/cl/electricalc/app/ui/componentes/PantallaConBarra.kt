package cl.electricalc.app.ui.componentes

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import cl.electricalc.app.R

/** Pantalla secundaria: barra con volver, título y acciones; el contenido recibe el modificador con el relleno. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConBarra(
    titulo: String,
    alVolver: () -> Unit,
    acciones: @Composable RowScope.() -> Unit = {},
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    contenido: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Los títulos largos del catálogo bajan un tamaño para caber en dos líneas junto a las acciones.
                    val estilo = if (titulo.length > 22) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
                    Text(titulo, style = estilo, maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                },
                actions = acciones,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { relleno ->
        contenido(Modifier.padding(relleno).consumeWindowInsets(relleno).imePadding())
    }
}
