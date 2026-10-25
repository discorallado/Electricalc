package cl.electricalc.app.ui.destinos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.ui.catalogo.Herramienta
import cl.electricalc.app.ui.componentes.ChipsRic
import cl.electricalc.app.ui.componentes.PantallaConBarra
import cl.electricalc.app.ui.componentes.Titular
import cl.electricalc.app.ui.componentes.nombreTipo

/** Fase de implementación según la prioridad de una herramienta nueva (`docs/arquitectura-v2.md`, sección 9). */
private fun fase(prioridad: Int?) = when (prioridad) {
    1 -> "C"
    2 -> "D"
    else -> "E"
}

/**
 * Herramienta que todavía no se puede usar: qué hará, en qué fase llega, sus pliegos y los datos
 * RIC que necesita. Nunca muestra valores normativos.
 */
@Composable
fun PantallaPendiente(h: Herramienta, alVolver: () -> Unit, alAbrirPliego: (Int) -> Unit) {
    PantallaConBarra(h.titulo, alVolver) { modificador ->
        Column(
            modificador.verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ChipsRic(h, alAbrirPliego, Modifier.padding(horizontal = 16.dp))
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Construction, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        if (h.nueva) {
                            stringResource(R.string.pendiente_nueva, stringResource(nombreTipo(h.tipo)), fase(h.prioridad))
                        } else {
                            stringResource(R.string.proximamente)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        h.descripcion ?: stringResource(R.string.proximamente_texto),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (h.datosRic.isNotEmpty()) {
                Titular(stringResource(R.string.datos_ric_necesarios))
                AvisoPendiente(stringResource(R.string.verificacion_ric_pendiente_texto))
                h.datosRic.forEach { AvisoPendiente(if (it.startsWith("RIC")) "• ${h.titulo} ($it)" else "• $it") }
            }
        }
    }
}
