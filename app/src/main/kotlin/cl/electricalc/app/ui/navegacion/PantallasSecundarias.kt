package cl.electricalc.app.ui.navegacion

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.datos.LocalPreferencias
import cl.electricalc.app.datos.Norma
import cl.electricalc.app.ui.componentes.PantallaConBarra
import cl.electricalc.app.ui.componentes.nombreSistema
import cl.electricalc.calc.comun.SistemaElectrico

/**
 * Idioma de la app (Android 13 o superior, con el selector por app del sistema). En versiones
 * anteriores la app sigue el idioma del teléfono.
 */
@Composable
private fun SelectorIdioma() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val gestor = LocalContext.current.getSystemService(LocaleManager::class.java) ?: return
    var actual by remember { mutableStateOf(gestor.applicationLocales.toLanguageTags()) }
    val opciones = listOf("" to R.string.idioma_sistema, "es-CL" to R.string.idioma_es, "en" to R.string.idioma_en)
    Text(stringResource(R.string.ajustes_idioma), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { i, (etiqueta, texto) ->
            SegmentedButton(
                selected = actual == etiqueta,
                onClick = {
                    actual = etiqueta
                    gestor.applicationLocales = LocaleList.forLanguageTags(etiqueta)
                },
                shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
            ) { Text(stringResource(texto), maxLines = 1) }
        }
    }
}

@Composable
private fun Interruptor(texto: Int, valor: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(texto), modifier = Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = alCambiar)
    }
}

@Composable
fun PantallaAjustes(alVolver: () -> Unit) {
    val p = LocalPreferencias.current
    PantallaConBarra(stringResource(R.string.ajustes), alVolver) { modificador ->
        Column(
            modificador.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.ajustes_valores), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.ajustes_valores_texto), style = MaterialTheme.typography.bodySmall)
            SistemaElectrico.entries.forEach { s ->
                OutlinedTextField(
                    value = p.tensionPorDefecto(s),
                    onValueChange = { p.fijarTensionPorDefecto(s, it) },
                    label = { Text(stringResource(R.string.ajustes_tension, stringResource(nombreSistema(s)))) },
                    suffix = { Text("V") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = p.cosPhiPorDefecto,
                onValueChange = p::fijarCosPhiPorDefecto,
                label = { Text(stringResource(R.string.ajustes_cos_phi)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.ajustes_unidades), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Interruptor(R.string.ajustes_pies, p.longitudEnPies, p::fijarLongitudEnPies)
            Interruptor(R.string.ajustes_awg, p.seccionEnAwg, p::fijarSeccionEnAwg)
            Interruptor(R.string.ajustes_fahrenheit, p.temperaturaEnFahrenheit, p::fijarTemperaturaEnFahrenheit)
            Text(stringResource(R.string.ajustes_norma), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.ajustes_norma_texto), style = MaterialTheme.typography.bodySmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Norma.entries.forEachIndexed { i, n ->
                    SegmentedButton(
                        selected = p.norma == n,
                        onClick = { p.fijarNorma(n) },
                        shape = SegmentedButtonDefaults.itemShape(i, Norma.entries.size),
                    ) { Text(n.name) }
                }
            }
            SelectorIdioma()
        }
    }
}

@Composable
private fun PantallaTexto(titulo: String, parrafos: List<Int>, alVolver: () -> Unit) {
    PantallaConBarra(titulo, alVolver) { modificador ->
        Column(
            modificador.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            parrafos.forEach {
                Card(Modifier.fillMaxWidth()) {
                    Text(stringResource(it), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun PantallaAcerca(alVolver: () -> Unit) =
    PantallaTexto(stringResource(R.string.acerca_de), listOf(R.string.acerca_texto, R.string.acerca_aviso), alVolver)

@Composable
fun PantallaGuiaUso(alVolver: () -> Unit) = PantallaTexto(
    stringResource(R.string.guia_uso),
    listOf(R.string.guia_uso_1, R.string.guia_uso_2, R.string.guia_uso_3, R.string.guia_uso_4),
    alVolver,
)
