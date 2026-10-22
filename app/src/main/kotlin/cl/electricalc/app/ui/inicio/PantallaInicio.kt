package cl.electricalc.app.ui.inicio

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import cl.electricalc.app.R
import cl.electricalc.app.ui.Rutas

data class EntradaMenu(
    @StringRes val titulo: Int,
    @StringRes val descripcion: Int,
    val icono: ImageVector,
    val ruta: String,
)

private val entradas = listOf(
    EntradaMenu(R.string.calc_ley_ohm, R.string.calc_ley_ohm_desc, Icons.Filled.Bolt, Rutas.LEY_OHM),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicio(alAbrir: (String) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { relleno ->
        LazyColumn(modifier = Modifier.padding(relleno)) {
            items(entradas) { entrada ->
                ListItem(
                    headlineContent = { Text(stringResource(entrada.titulo)) },
                    supportingContent = { Text(stringResource(entrada.descripcion)) },
                    leadingContent = { Icon(entrada.icono, contentDescription = null) },
                    modifier = Modifier.clickable { alAbrir(entrada.ruta) },
                )
            }
        }
    }
}
