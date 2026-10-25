package cl.electricalc.app.ui.componentes

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.ui.calculadoras.Formato
import cl.electricalc.app.ui.calculadoras.LineaResultado
import cl.electricalc.calc.comun.SistemaElectrico

@StringRes
fun nombreSistema(s: SistemaElectrico): Int = when (s) {
    SistemaElectrico.CONTINUA -> R.string.sistema_continua
    SistemaElectrico.MONOFASICA -> R.string.sistema_monofasica
    SistemaElectrico.BIFASICA -> R.string.sistema_bifasica
    SistemaElectrico.TRIFASICA -> R.string.sistema_trifasica
}

/** Botón ⓘ que abre la explicación de un campo. */
@Composable
fun BotonAyuda(descripcion: String, alPulsar: () -> Unit) {
    IconButton(onClick = alPulsar, modifier = Modifier.size(36.dp)) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = stringResource(R.string.ayuda_de, descripcion),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Control segmentado CC / monofásica / bifásica / trifásica, con su título y ayuda. */
@Composable
fun SelectorTipoCorriente(
    sistemas: List<SistemaElectrico>,
    seleccionado: SistemaElectrico,
    alElegir: (SistemaElectrico) -> Unit,
    alAyuda: () -> Unit,
) {
    val titulo = stringResource(R.string.campo_tipo_corriente)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleSmall)
        BotonAyuda(titulo, alAyuda)
    }
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        sistemas.forEachIndexed { i, s ->
            SegmentedButton(
                selected = s == seleccionado,
                onClick = { alElegir(s) },
                shape = SegmentedButtonDefaults.itemShape(i, sistemas.size),
                icon = {},
            ) {
                Text(stringResource(nombreSistema(s)), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** Lista desplegable de solo lectura. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Desplegable(
    opciones: List<String>,
    seleccion: Int,
    alElegir: (Int) -> Unit,
    modifier: Modifier = Modifier,
    descripcion: String? = null,
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }, modifier = modifier) {
        OutlinedTextField(
            value = opciones.getOrElse(seleccion) { "" },
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = descripcion?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEachIndexed { i, texto ->
                DropdownMenuItem(
                    text = { Text(texto) },
                    onClick = {
                        alElegir(i)
                        abierto = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/** Selector de unidad: caja angosta con el símbolo y una flecha pequeña, alineada con el campo de texto. */
@Composable
fun SelectorUnidad(simbolos: List<String>, seleccion: Int, alElegir: (Int) -> Unit, modifier: Modifier = Modifier) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedCard(
            onClick = { abierto = true },
            shape = MaterialTheme.shapes.extraSmall,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Row(
                Modifier.fillMaxSize().padding(start = 10.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    simbolos.getOrElse(seleccion) { "" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            simbolos.forEachIndexed { i, s ->
                DropdownMenuItem(text = { Text(s) }, onClick = {
                    alElegir(i)
                    abierto = false
                })
            }
        }
    }
}

/** Selector "Datos conocidos" para las pantallas con varias combinaciones de entrada. */
@Composable
fun SelectorVariante(nombres: List<String>, seleccion: Int, alElegir: (Int) -> Unit) {
    Desplegable(
        nombres, seleccion, alElegir,
        modifier = Modifier.fillMaxWidth(),
        descripcion = stringResource(R.string.datos_conocidos),
    )
}

/**
 * Fila de la tabla de entradas: etiqueta con su ⓘ a la izquierda y el control a la derecha
 * (campo numérico más unidad, o un desplegable).
 */
@Composable
fun FilaEntrada(etiqueta: String, alAyuda: () -> Unit, contenido: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(0.38f), verticalAlignment = Alignment.CenterVertically) {
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
            BotonAyuda(etiqueta, alAyuda)
        }
        Row(
            Modifier.weight(0.62f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = contenido,
        )
    }
}

/** Campo numérico con hint. Marca error si el texto no es un número. */
@Composable
fun CampoNumero(valor: String, alCambiar: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        placeholder = { Text(hint) },
        singleLine = true,
        isError = valor.isNotBlank() && Formato.leer(valor) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = modifier,
    )
}

/** Unidad fija (sin alternativas) alineada con los selectores. */
@Composable
fun UnidadFija(simbolo: String, modifier: Modifier = Modifier) {
    Text(simbolo, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = modifier)
}

/** Explicación de un campo (título y párrafos). */
@Composable
fun DialogoAyuda(titulo: String, textos: List<Int>, alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                textos.forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.entendido)) } },
    )
}

/** Tarjeta animada con los resultados: los principales destacados y los secundarios debajo. */
@Composable
fun TarjetaResultado(lineas: List<LineaResultado>?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = lineas != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        // Se conserva la última lista mientras se anima la salida.
        var ultima by remember { mutableStateOf(lineas.orEmpty()) }
        if (lineas != null) ultima = lineas
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.resultado),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.size(8.dp))
                val (principales, secundarias) = ultima.partition { it.principal }
                principales.forEach { linea ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(linea.etiqueta),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            linea.valor,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                if (secundarias.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    secundarias.forEach { linea ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                stringResource(linea.etiqueta),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            Text(linea.valor, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}
