package cl.electricalc.app.ui.proyecto

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.datos.CalculoProyecto
import cl.electricalc.app.datos.Carga
import cl.electricalc.app.datos.LocalPreferencias
import cl.electricalc.app.datos.Proyecto
import cl.electricalc.app.datos.ResultadoProyecto
import cl.electricalc.app.datos.TipoCarga
import cl.electricalc.app.ui.calculadoras.Formato
import cl.electricalc.app.ui.calculadoras.Informe
import cl.electricalc.app.ui.componentes.Desplegable
import cl.electricalc.app.ui.componentes.PantallaConBarra
import cl.electricalc.app.ui.componentes.Titular
import cl.electricalc.datos.ric.Ric

private fun nombreTipo(t: TipoCarga) = when (t) {
    TipoCarga.ILUMINACION -> R.string.carga_iluminacion
    TipoCarga.ENCHUFES -> R.string.carga_enchufes
    TipoCarga.FUERZA -> R.string.carga_fuerza
    TipoCarga.CLIMATIZACION -> R.string.carga_climatizacion
}

private fun kw(w: Double) = "${Formato.numero(w / 1000, 2)} kW"

/** Memoria de cálculo en PDF: datos, cuadro de cargas, resultados por etapa y lo que pide el RIC N°18. */
private fun memoria(c: Context, p: Proyecto, r: ResultadoProyecto): Informe {
    val sistema = c.getString(if (p.trifasico) R.string.sistema_trifasica else R.string.sistema_monofasica)
    return Informe(
        c.getString(R.string.memoria_titulo, p.nombre),
        listOf(
            c.getString(R.string.memoria_datos) to listOf(
                c.getString(R.string.campo_tipo_consumidor) to p.consumidor,
                c.getString(R.string.campo_tipo_corriente) to sistema,
                c.getString(R.string.campo_tension) to "${Formato.numero(p.tensionV, 0)} V",
                c.getString(R.string.campo_factor_potencia) to Formato.numero(p.cosPhi, 2),
            ),
            c.getString(R.string.cuadro_cargas) to p.cargas.map {
                "${it.nombre} (${c.getString(nombreTipo(it.tipo))})" to "${it.cantidad} × ${Formato.numero(it.potenciaW, 0)} W = ${kw(it.totalW)}"
            },
            c.getString(R.string.memoria_resultados) to listOfNotNull(
                c.getString(R.string.res_potencia_instalada) to kw(r.instaladaW),
                c.getString(R.string.res_demanda_maxima) to kw(r.demandaW) + " (RIC N°03, tabla N°3.1)",
                c.getString(R.string.res_corriente_diseno) to "${Formato.numero(r.corrienteA, 2)} A (RIC N°07, 5.5.5)",
                r.empalme?.let { c.getString(R.string.res_empalme) to "${Formato.numero(it.a, 0)} A · ${Formato.numero(it.kw, 1)} kW (RIC N°01, anexo 1.3)" },
                r.alimentador?.let { c.getString(R.string.res_seccion_alimentador) to "${Formato.numero(it.mm2, 2)} mm² · Ic ${Formato.numero(it.ic, 1)} A (RIC N°04, tabla N°4.4)" },
            ),
            c.getString(R.string.memoria_requisitos) to
                (if (r.memoriaObligatoria) listOf(c.getString(R.string.memoria_obligatoria) to "") else emptyList()) +
                Ric.proyecto.partes.map { "☐ $it" to "" } + r.calculosMemoria.map { "☐ $it" to "" },
            c.getString(R.string.memoria_aviso_titulo) to listOf(c.getString(R.string.memoria_aviso) to ""),
        ),
    )
}

@Composable
private fun DialogoCarga(alCerrar: () -> Unit, alAgregar: (Carga) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(0) }
    var potencia by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    val tipos = TipoCarga.entries
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.agregar_carga)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text(stringResource(R.string.campo_nombre)) }, singleLine = true)
                Desplegable(tipos.map { stringResource(nombreTipo(it)) }, tipo, { tipo = it }, Modifier.fillMaxWidth())
                OutlinedTextField(
                    potencia, { potencia = it }, label = { Text(stringResource(R.string.campo_potencia_unitaria_w)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    cantidad, { cantidad = it }, label = { Text(stringResource(R.string.campo_cantidad)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            val p = Formato.leer(potencia)
            val n = cantidad.toIntOrNull()
            TextButton(
                enabled = p != null && p > 0 && n != null && n > 0,
                onClick = { alAgregar(Carga(nombre.ifBlank { "Carga" }, tipos[tipo], p!!, n!!)) },
            ) { Text(stringResource(R.string.agregar)) }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/** Proyecto por etapas: datos comunes, cuadro de cargas y resultados que pasan de una etapa a la siguiente. */
@Composable
fun PantallaProyecto(id: String, alVolver: () -> Unit) {
    val preferencias = LocalPreferencias.current
    val contexto = LocalContext.current
    val p = preferencias.proyecto(id)
    if (p == null) {
        LaunchedEffect(id) { alVolver() }
        return
    }
    var tensionTexto by remember(id) { mutableStateOf(Formato.numero(p.tensionV, 0)) }
    var cosTexto by remember(id) { mutableStateOf(Formato.numero(p.cosPhi, 2)) }
    var agregando by remember { mutableStateOf(false) }
    fun guardar(nuevo: Proyecto) = preferencias.guardarProyecto(nuevo)
    val r = CalculoProyecto.calcular(p)
    val consumidores = Ric.factoresDemanda.consumidores.map { it.tipo }

    PantallaConBarra(
        p.nombre, alVolver,
        acciones = {
            IconButton(onClick = { memoria(contexto, p, r).compartirPdf(contexto) }) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = stringResource(R.string.exportar_memoria))
            }
            IconButton(onClick = { preferencias.eliminarProyecto(id); alVolver() }) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.eliminar_proyecto))
            }
        },
    ) { m ->
        Column(m.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                p.nombre, { guardar(p.copy(nombre = it)) }, label = { Text(stringResource(R.string.campo_nombre)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Desplegable(
                consumidores, consumidores.indexOf(p.consumidor).coerceAtLeast(0),
                { guardar(p.copy(consumidor = consumidores[it])) }, Modifier.fillMaxWidth(),
                descripcion = stringResource(R.string.campo_tipo_consumidor),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(false, true).forEachIndexed { i, tri ->
                    SegmentedButton(
                        selected = p.trifasico == tri,
                        onClick = {
                            val v = if (tri) 380.0 else 220.0
                            tensionTexto = Formato.numero(v, 0)
                            guardar(p.copy(trifasico = tri, tensionV = v))
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(stringResource(if (tri) R.string.sistema_trifasica else R.string.sistema_monofasica)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    tensionTexto, { v -> tensionTexto = v; Formato.leer(v)?.takeIf { it > 0 }?.let { guardar(p.copy(tensionV = it)) } },
                    label = { Text(stringResource(R.string.campo_tension)) }, suffix = { Text("V") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    cosTexto, { v -> cosTexto = v; Formato.leer(v)?.takeIf { it in 0.01..1.0 }?.let { guardar(p.copy(cosPhi = it)) } },
                    label = { Text(stringResource(R.string.campo_factor_potencia)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.campo_es_vivienda), modifier = Modifier.weight(1f))
                Switch(p.esVivienda, { guardar(p.copy(esVivienda = it)) })
            }

            Titular(stringResource(R.string.etapa_1_cuadro), Modifier.padding(start = 0.dp))
            Card(Modifier.fillMaxWidth()) {
                Column {
                    if (p.cargas.isEmpty()) {
                        Text(stringResource(R.string.sin_cargas), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline)
                    }
                    p.cargas.forEachIndexed { i, c ->
                        ListItem(
                            headlineContent = { Text(c.nombre) },
                            supportingContent = { Text("${stringResource(nombreTipo(c.tipo))} · ${c.cantidad} × ${Formato.numero(c.potenciaW, 0)} W") },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(kw(c.totalW))
                                    IconButton(onClick = { guardar(p.copy(cargas = p.cargas.filterIndexed { j, _ -> j != i })) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.quitar))
                                    }
                                }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    TextButton(onClick = { agregando = true }, modifier = Modifier.padding(8.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.agregar_carga), Modifier.padding(start = 8.dp))
                    }
                }
            }

            Titular(stringResource(R.string.resultados_etapas), Modifier.padding(start = 0.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    @Composable
                    fun fila(etiqueta: Int, valor: String) = Row {
                        Text(stringResource(etiqueta), Modifier.weight(1f))
                        Text(valor, style = MaterialTheme.typography.titleSmall)
                    }
                    fila(R.string.res_potencia_instalada, kw(r.instaladaW))
                    fila(R.string.res_demanda_maxima, kw(r.demandaW))
                    fila(R.string.res_corriente_diseno, "${Formato.numero(r.corrienteA, 2)} A")
                    fila(R.string.res_empalme, r.empalme?.let { "${Formato.numero(it.a, 0)} A · ${Formato.numero(it.kw, 1)} kW" } ?: "—")
                    fila(R.string.res_seccion_alimentador, r.alimentador?.let { "${Formato.numero(it.mm2, 2)} mm²" } ?: "—")
                    Text(stringResource(R.string.nota_alimentador), style = MaterialTheme.typography.bodySmall)
                }
            }

            Titular(stringResource(R.string.memoria_requisitos), Modifier.padding(start = 0.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (r.memoriaObligatoria) Text(stringResource(R.string.memoria_obligatoria), style = MaterialTheme.typography.titleSmall)
                    (Ric.proyecto.partes + r.calculosMemoria).forEach { Text("☐ $it", style = MaterialTheme.typography.bodyMedium) }
                    Text(stringResource(R.string.memoria_cita), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Button(onClick = { memoria(contexto, p, r).compartirPdf(contexto) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                Text(stringResource(R.string.exportar_memoria), Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = { memoria(contexto, p, r).compartirTexto(contexto) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.compartir))
            }
        }
    }
    if (agregando) {
        DialogoCarga(alCerrar = { agregando = false }) { c ->
            guardar(p.copy(cargas = p.cargas + c))
            agregando = false
        }
    }
}
