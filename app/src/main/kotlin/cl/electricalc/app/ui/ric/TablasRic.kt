package cl.electricalc.app.ui.ric

import androidx.annotation.StringRes
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.ui.calculadoras.Formato
import cl.electricalc.app.ui.componentes.PantallaConBarra
import cl.electricalc.app.ui.componentes.citaRic
import cl.electricalc.datos.ric.CaidasAdmisibles
import cl.electricalc.datos.ric.CapacidadCobreAislado
import cl.electricalc.datos.ric.CapacidadUsoMovil
import cl.electricalc.datos.ric.ColoresConductores
import cl.electricalc.datos.ric.FactorCantidadConductores
import cl.electricalc.datos.ric.FactorTemperatura
import cl.electricalc.datos.ric.FactoresDemanda
import cl.electricalc.datos.ric.FactoresSimultaneidad
import cl.electricalc.datos.ric.FuenteRic
import cl.electricalc.datos.ric.MedicionResistividad
import cl.electricalc.datos.ric.ResistenciaElectrodos
import cl.electricalc.datos.ric.ResistividadTerrenos
import cl.electricalc.datos.ric.SeccionProteccion
import cl.electricalc.datos.ric.Ric
import cl.electricalc.datos.ric.SeccionesMinimas

/** Tablas RIC transcritas, por clave de ruta; y las herramientas del catálogo que las muestran. */
object TablasRic {
    val porClave: Map<String, FuenteRic> by lazy {
        mapOf(
            "caidas" to Ric.caidas, "demanda" to Ric.factoresDemanda, "simultaneidad" to Ric.simultaneidad,
            "secciones" to Ric.seccionesMinimas, "colores" to Ric.colores, "capacidad" to Ric.capacidad,
            "capacidad_movil" to Ric.capacidadMovil, "fn" to Ric.fn, "ft" to Ric.ft,
            "resistividad" to Ric.resistividad, "electrodos" to Ric.electrodos,
            "seccion_proteccion" to Ric.seccionProteccion, "medicion_resistividad" to Ric.medicionResistividad,
        )
    }

    /** Herramientas nuevas de tipo tabla que ya tienen sus datos transcritos. */
    private val herramientas = mapOf(
        "tabla_caidas_admisibles" to "caidas",
        "tabla_secciones_minimas" to "secciones",
        "tabla_colores_ric" to "colores",
    )

    fun claveDeHerramienta(id: String): String? = herramientas[id]

    fun clave(f: FuenteRic): String = porClave.entries.first { it.value === f }.key
}

@StringRes
private fun usoTexto(uso: String) = when (uso) {
    "iluminacion" -> R.string.uso_iluminacion
    "enchufes" -> R.string.uso_enchufes
    "mixto" -> R.string.uso_mixto
    "subalimentador" -> R.string.uso_subalimentador
    else -> R.string.uso_alimentador
}

/** Fila de una tabla con columnas de ancho fijo (para que las cifras queden alineadas). */
@Composable
private fun Fila(celdas: List<String>, anchos: List<Dp>, cabecera: Boolean = false) {
    Row(Modifier.padding(vertical = 4.dp)) {
        celdas.forEachIndexed { i, c ->
            Text(
                c,
                fontWeight = if (cabecera) FontWeight.SemiBold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                modifier = Modifier.width(anchos.getOrElse(i) { anchos.last() }),
            )
        }
    }
}

@Composable
private fun Tabla(cabecera: List<String>, filas: List<List<String>>, anchos: List<Dp>) {
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Fila(cabecera, anchos, cabecera = true)
        HorizontalDivider()
        filas.forEach { Fila(it, anchos) }
    }
}

private fun n(v: Double?, decimales: Int = 2) = v?.let { Formato.numero(it, decimales) } ?: "–"

/** Encabezado común: título, cita, páginas y estado de revisión. */
@Composable
fun EncabezadoFuente(f: FuenteRic) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(f.titulo, style = MaterialTheme.typography.titleMedium)
        Text(citaRic(f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(R.string.fuente_paginas, f.version, f.paginas.joinToString(", ")),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (f.revision.pendiente) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.fuente_en_revision), style = MaterialTheme.typography.labelLarge)
                    f.revision.observaciones.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

/** Contenido de cada tabla. Los textos de las celdas son los del pliego. */
@Composable
fun ContenidoFuente(f: FuenteRic) {
    val ancho = 64.dp
    when (f) {
        is CaidasAdmisibles -> Tabla(
            listOf(stringResource(R.string.col_requisito), stringResource(R.string.col_limite)),
            listOf(
                listOf(stringResource(R.string.ver_caida_tramo), "${n(f.porTramo)} %"),
                listOf(stringResource(R.string.ver_caida_total), "${n(f.total)} %"),
            ),
            listOf(240.dp, 72.dp),
        )
        is SeccionesMinimas -> Tabla(
            listOf(stringResource(R.string.col_uso), stringResource(R.string.col_seccion_minima)),
            f.usos.map { listOf(stringResource(usoTexto(it.uso)), "${n(it.mm2, 1)} mm²") },
            listOf(220.dp, 100.dp),
        )
        is ColoresConductores -> Tabla(
            listOf(stringResource(R.string.col_conductor), stringResource(R.string.col_color)),
            f.conductores.map { listOf(it.nombre, it.color) },
            listOf(220.dp, 140.dp),
        )
        is FactorCantidadConductores -> Tabla(
            listOf(stringResource(R.string.col_conductores), "fn"),
            f.rangos.map { listOf(if (it.hasta == null) "≥ ${it.desde}" else "${it.desde}–${it.hasta}", n(it.factor)) },
            listOf(160.dp, ancho),
        )
        is FactorTemperatura -> Tabla(
            listOf("°C") + f.columnas.map { "${it.temperaturaServicio} °C ${it.metodos.joinToString("/")}" },
            f.filas.map { fila -> listOf("${fila.desde}–${fila.hasta}") + f.columnas.map { n(fila.factores[it.id]) } },
            listOf(64.dp, 150.dp),
        )
        is FactoresDemanda -> Tabla(
            listOf(stringResource(R.string.col_consumidor), stringResource(R.string.col_tramo_kw), "Fd"),
            f.consumidores.flatMap { c ->
                var desde = 0.0
                c.tramos.mapIndexed { i, t ->
                    val tramo = when {
                        t.hastaKw == null && i == 0 -> stringResource(R.string.toda_la_potencia)
                        t.hastaKw == null -> "> ${n(desde, 1)}"
                        else -> "${n(desde, 1)}–${n(t.hastaKw, 1)}"
                    }
                    desde = t.hastaKw ?: desde
                    listOf(if (i == 0) c.tipo else "", tramo, n(t.factor))
                }
            },
            listOf(200.dp, 110.dp, ancho),
        )
        is FactoresSimultaneidad -> Tabla(
            listOf(stringResource(R.string.col_viviendas), "Fs"),
            f.porCantidad.mapIndexed { i, v -> listOf("${i + 1}", n(v)) } + listOf(listOf("n > 21", "(15,3 + (n − 21) × 0,5) / n")),
            listOf(100.dp, 200.dp),
        )
        is CapacidadUsoMovil -> Tabla(
            listOf("mm²", "AWG", "${f.metodo} ${f.temperaturaAmbiente} °C"),
            f.filas.map { listOf(n(it.mm2, 2), it.awg ?: "–", n(it.amperes, 1)) },
            listOf(72.dp, 56.dp, 96.dp),
        )
        is ResistividadTerrenos -> {
            Tabla(
                listOf(stringResource(R.string.col_terreno), "Ω·m"),
                f.terrenos.map { listOf(it.terreno, it.texto ?: if (it.minimo == it.maximo) n(it.maximo, 0) else "${n(it.minimo, 0)} – ${n(it.maximo, 0)}") },
                listOf(240.dp, 120.dp),
            )
            Text(f.valoresMedios.titulo + " (tabla N°" + f.valoresMedios.tabla + ")", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Tabla(
                listOf(stringResource(R.string.col_terreno), "Ω·m"),
                f.valoresMedios.terrenos.map { listOf(it.terreno, n(it.valor, 0)) },
                listOf(240.dp, 80.dp),
            )
        }
        is ResistenciaElectrodos -> Tabla(
            listOf(stringResource(R.string.campo_electrodo), stringResource(R.string.col_formula)),
            f.electrodos.map { listOf(it.nombre, it.formula) },
            listOf(220.dp, 120.dp),
        )
        is SeccionProteccion -> Tabla(
            listOf(stringResource(R.string.col_seccion_fase), stringResource(R.string.col_seccion_proteccion)),
            f.tramos.fold(0.0 to listOf<List<String>>()) { (desde, filas), t ->
                val rango = if (t.faseHasta == null) "S > ${n(desde, 0)}" else if (desde == 0.0) "S ≤ ${n(t.faseHasta, 0)}" else "${n(desde, 0)} < S ≤ ${n(t.faseHasta, 0)}"
                (t.faseHasta ?: desde) to (filas + listOf(listOf(rango, t.proteccion)))
            }.second,
            listOf(160.dp, 120.dp),
        )
        is MedicionResistividad -> Tabla(
            listOf(stringResource(R.string.col_requisito), stringResource(R.string.col_limite)),
            listOf(
                listOf(stringResource(R.string.col_metodos), f.metodos.joinToString(" o ")),
                listOf(stringResource(R.string.col_ala_maxima), "${n(f.alaMaximaM, 0)} m"),
                listOf(stringResource(R.string.col_ala_reducida), "${n(f.alaReducidaM, 0)} m"),
                listOf(stringResource(R.string.col_ala_excepcional), "${n(f.alaExcepcionalM, 0)} m"),
            ),
            listOf(220.dp, 160.dp),
        )
        is CapacidadCobreAislado -> {
            var indice by rememberSaveable { mutableIntStateOf(0) }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                f.bloques.forEachIndexed { i, b ->
                    SegmentedButton(
                        selected = i == indice,
                        onClick = { indice = i },
                        shape = SegmentedButtonDefaults.itemShape(i, f.bloques.size),
                    ) { Text(stringResource(R.string.servicio_a, b.temperaturaServicio)) }
                }
            }
            val b = f.bloques[indice]
            Text(
                stringResource(R.string.aplica_a, b.cables.joinToString(", ")),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Tabla(
                listOf("mm²", "AWG") + b.metodos.map { "$it ${b.temperaturaAmbiente[it]}°" },
                b.filas.map { fila -> listOf(n(fila.mm2, 2), fila.awg ?: "–") + b.metodos.map { m -> fila.amperes[m]?.toString() ?: "–" } },
                listOf(64.dp, 48.dp, ancho),
            )
            f.metodos.filterKeys { it in b.metodos }.forEach { (m, texto) ->
                Text("$m: $texto", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Pantalla de una tabla transcrita: referencia, estado de revisión, valores y notas del pliego. */
@Composable
fun PantallaTablaRic(f: FuenteRic, alVolver: () -> Unit) {
    PantallaConBarra(citaRic(f), alVolver) { modificador ->
        Column(
            modificador.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EncabezadoFuente(f)
            ContenidoFuente(f)
            val notas = notasDe(f)
            if (notas.isNotEmpty()) {
                Text(stringResource(R.string.notas_pliego), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                notas.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun notasDe(f: FuenteRic): List<String> = when (f) {
    is CaidasAdmisibles -> f.notas
    is FactoresDemanda -> f.notas
    is FactoresSimultaneidad -> f.notas
    is SeccionesMinimas -> f.notas
    is ColoresConductores -> f.notas
    is CapacidadCobreAislado -> f.notas
    is CapacidadUsoMovil -> f.notas
    is FactorCantidadConductores -> f.notas
    is FactorTemperatura -> f.notas
    is ResistividadTerrenos -> f.notas
    is ResistenciaElectrodos -> f.notas
    is SeccionProteccion -> f.notas
    is MedicionResistividad -> f.notas
    else -> emptyList()
}
