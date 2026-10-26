package cl.electricalc.app.ui.calculadoras

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.electricalc.app.R
import cl.electricalc.app.datos.LocalPreferencias
import cl.electricalc.app.ui.catalogo.Herramienta
import cl.electricalc.app.ui.componentes.ChipsRic
import cl.electricalc.app.ui.componentes.CampoNumero
import cl.electricalc.app.ui.componentes.Desplegable
import cl.electricalc.app.ui.componentes.DialogoAyuda
import cl.electricalc.app.ui.componentes.FilaEntrada
import cl.electricalc.app.ui.componentes.PantallaConBarra
import cl.electricalc.app.ui.componentes.SelectorTipoCorriente
import cl.electricalc.app.ui.componentes.SelectorUnidad
import cl.electricalc.app.ui.componentes.SelectorVariante
import cl.electricalc.app.ui.componentes.TarjetaResultado
import cl.electricalc.app.ui.componentes.TarjetaVerificacion
import cl.electricalc.app.ui.componentes.UnidadFija
import cl.electricalc.app.ui.componentes.nombreSistema
import cl.electricalc.app.ui.formulario.Fichas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Nombre de una combinación de datos: "Tensión y potencia". */
private fun nombreVariante(contexto: Context, calculadora: Calculadora, variante: Variante): String {
    variante.nombre?.let { return contexto.getString(it) }
    val nombres = variante.campos.mapIndexed { i, id ->
        val texto = contexto.getString(calculadora.campos.first { it.id == id }.etiqueta)
        if (i == 0) texto else texto.lowercase()
    }
    return when (nombres.size) {
        1 -> nombres[0]
        2 -> contexto.getString(R.string.variante_2, nombres[0], nombres[1])
        else -> contexto.getString(R.string.variante_3, nombres[0], nombres[1], nombres[2])
    }
}

private fun textoOpcion(contexto: Context, opcion: TextoOpcion) = when (opcion) {
    is TextoOpcion.Recurso -> contexto.getString(opcion.id)
    is TextoOpcion.Literal -> opcion.texto
}

/** Datos visibles y resultados, con sus unidades, para compartir. */
private fun informe(contexto: Context, titulo: String, estado: EstadoCalculadora): Informe {
    val def = estado.definicion
    val datos = buildList {
        if (def.sistemas.size > 1) {
            add(contexto.getString(R.string.campo_tipo_corriente) to contexto.getString(nombreSistema(estado.sistema)))
        }
        estado.camposVisibles.forEach { c ->
            val valor = when (c) {
                // El desfase se lee "cos φ = 0,9"; el resto, valor y unidad ("380 V").
                is CampoNumerico -> if (c.id == Ids.FASE) {
                    "${estado.unidad(c).simbolo} = ${estado.textos[c.id].orEmpty()}"
                } else {
                    "${estado.textos[c.id].orEmpty()} ${estado.unidad(c).simbolo}".trim()
                }
                is CampoOpciones -> textoOpcion(contexto, c.opciones[estado.opcion(c.id)])
                is CampoSeccion -> "${estado.opcionesSeccion(c)[estado.opcion(c.id)]} ${estado.unidadSeccion(c).simbolo}"
            }
            add(contexto.getString(c.etiqueta) to valor)
        }
    }
    val resultados = estado.resultado.orEmpty().map { contexto.getString(it.etiqueta) to it.valor }
    return Informe(contexto, titulo, datos, resultados)
}

/** Pantalla de una calculadora, armada a partir de su definición declarativa. */
@Composable
fun PantallaCalculadora(
    calculadora: Calculadora,
    item: Herramienta,
    alVolver: () -> Unit,
    alAbrirFicha: (String) -> Unit,
    alAbrirGuia: () -> Unit,
    alAbrirPliego: (Int) -> Unit,
) {
    val preferencias = LocalPreferencias.current
    val estado: EstadoCalculadora = viewModel(key = calculadora.id) { EstadoCalculadora(calculadora, preferencias) }
    val contexto = LocalContext.current
    val foco = LocalFocusManager.current
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val desplazamiento = rememberScrollState()
    var ayuda by remember { mutableStateOf<Pair<String, List<Int>>?>(null) }
    var menu by remember { mutableStateOf(false) }

    // Al aparecer un resultado se baja hasta la tarjeta.
    LaunchedEffect(estado.resultado) {
        if (estado.resultado != null) {
            delay(250)
            desplazamiento.animateScrollTo(desplazamiento.maxValue)
        }
    }

    fun avisarSinResultado() {
        alcance.launch { snackbar.showSnackbar(contexto.getString(R.string.calcula_primero)) }
    }

    PantallaConBarra(
        titulo = item.titulo,
        alVolver = alVolver,
        snackbar = snackbar,
        acciones = {
            if (Fichas.existe(item.formula)) {
                IconButton(onClick = { alAbrirFicha(item.formula!!) }) {
                    Icon(Icons.Filled.Functions, contentDescription = stringResource(R.string.formula))
                }
            }
            IconButton(onClick = alAbrirGuia) {
                Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = stringResource(R.string.guia))
            }
            val favorito = preferencias.esFavorito(item.id)
            IconButton(onClick = { preferencias.alternarFavorito(item.id) }) {
                Icon(
                    if (favorito) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = stringResource(if (favorito) R.string.quitar_favorito else R.string.agregar_favorito),
                )
            }
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.mas_opciones))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.exportar_pdf)) },
                    onClick = {
                        menu = false
                        if (estado.resultado == null) avisarSinResultado() else informe(contexto, item.titulo, estado).compartirPdf(contexto)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.compartir)) },
                    onClick = {
                        menu = false
                        if (estado.resultado == null) avisarSinResultado() else informe(contexto, item.titulo, estado).compartirTexto(contexto)
                    },
                )
            }
        },
    ) { modificador ->
        Column(
            modificador.verticalScroll(desplazamiento).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChipsRic(item, alAbrirPliego)

            if (calculadora.sistemas.size > 1) {
                Column {
                    SelectorTipoCorriente(
                        calculadora.sistemas, estado.sistema, estado::elegirSistema,
                        alAyuda = {
                            ayuda = contexto.getString(R.string.campo_tipo_corriente) to listOf(R.string.ayuda_tipo_corriente)
                        },
                    )
                }
            }

            val variantes = estado.variantes
            if (variantes.size > 1) {
                SelectorVariante(
                    variantes.map { nombreVariante(contexto, calculadora, it) },
                    estado.indiceVariante,
                    estado::elegirVariante,
                )
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    estado.camposVisibles.forEach { campo ->
                        val etiqueta = stringResource(campo.etiqueta)
                        FilaEntrada(etiqueta, alAyuda = { ayuda = etiqueta to campo.ayuda }) {
                            when (campo) {
                                is CampoNumerico -> {
                                    CampoNumero(
                                        valor = estado.textos[campo.id].orEmpty(),
                                        alCambiar = { estado.escribir(campo.id, it) },
                                        hint = campo.hint(estado.sistema),
                                        modifier = Modifier.weight(0.64f),
                                    )
                                    val unidades = estado.unidades(campo)
                                    if (unidades.size == 1) {
                                        UnidadFija(unidades[0].simbolo, Modifier.weight(0.36f))
                                    } else {
                                        SelectorUnidad(
                                            unidades.map { it.simbolo },
                                            unidades.indexOf(estado.unidad(campo)),
                                            { estado.elegirUnidad(campo.id, unidades[it].simbolo) },
                                            Modifier.weight(0.36f),
                                        )
                                    }
                                }
                                is CampoOpciones -> Desplegable(
                                    campo.opciones.map { textoOpcion(contexto, it) },
                                    estado.opcion(campo.id),
                                    { estado.elegirOpcion(campo.id, it) },
                                    Modifier.weight(1f),
                                )
                                is CampoSeccion -> {
                                    Desplegable(
                                        estado.opcionesSeccion(campo),
                                        estado.opcion(campo.id),
                                        { estado.elegirOpcion(campo.id, it) },
                                        Modifier.weight(0.64f),
                                    )
                                    val simbolos = listOf(CalculadoraCaida.MM2, CalculadoraCaida.AWG)
                                    SelectorUnidad(
                                        simbolos,
                                        simbolos.indexOf(estado.unidadSeccion(campo).simbolo),
                                        { estado.elegirUnidadSeccion(simbolos[it]) },
                                        Modifier.weight(0.36f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    foco.clearFocus()
                    estado.calcular()?.let { error ->
                        alcance.launch { snackbar.showSnackbar(contexto.getString(error)) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(stringResource(R.string.calcular))
            }

            TarjetaResultado(estado.resultado)
            if (estado.resultado != null) TarjetaVerificacion(estado.verificaciones, alAbrirPliego)
            Spacer(Modifier.height(24.dp))
        }
    }

    ayuda?.let { (titulo, textos) -> DialogoAyuda(titulo, textos) { ayuda = null } }
}
