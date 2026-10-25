package cl.electricalc.app.ui.formulario

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.ui.componentes.PantallaConBarra

/** Ficha del formulario: una tarjeta por tipo de corriente con sus fórmulas y la leyenda al final. */
@Composable
fun PantallaFicha(id: String, titulo: String, alVolver: () -> Unit) {
    val ficha = remember(id) { Fichas.ficha(id) }
    PantallaConBarra(titulo, alVolver) { modificador ->
        if (ficha == null) {
            Text(stringResource(R.string.proximamente_texto), modifier = modificador.padding(16.dp))
            return@PantallaConBarra
        }
        Column(
            modificador.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val estilo = MaterialTheme.typography.titleMedium
            ficha.tarjetas.forEach { tarjeta ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(tarjeta.titulo),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        tarjeta.formulas.forEachIndexed { i, f ->
                            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
                                VistaFormula(f, estilo)
                            }
                        }
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.leyenda),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ficha.leyenda.forEach { s ->
                        Row {
                            Text(
                                textoConIndices(s.simbolo),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(72.dp),
                            )
                            val descripcion = stringResource(s.descripcion)
                            Text(if (s.unidad != null) "$descripcion [${s.unidad}]" else descripcion)
                        }
                    }
                    ficha.notas.forEach {
                        Text(stringResource(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}
