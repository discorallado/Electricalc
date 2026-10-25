package cl.electricalc.app.ui.guia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.ui.calculadoras.Calculadora
import cl.electricalc.app.ui.componentes.PantallaConBarra

/** Guía de una calculadora: qué calcula y una tarjeta "parámetro → explicación" por dato. */
@Composable
fun PantallaGuia(calculadora: Calculadora, titulo: String, alVolver: () -> Unit) {
    val parametros = buildList {
        if (calculadora.sistemas.size > 1) add(R.string.campo_tipo_corriente to listOf(R.string.ayuda_tipo_corriente))
        calculadora.campos.distinctBy { it.etiqueta }.forEach { add(it.etiqueta to it.ayuda) }
    }
    PantallaConBarra(stringResource(R.string.guia_de, titulo), alVolver) { modificador ->
        LazyColumn(
            modificador,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(calculadora.guia.intro) {
                Text(stringResource(it), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 4.dp))
            }
            items(parametros) { (nombre, textos) ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(stringResource(nombre), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        textos.forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}
