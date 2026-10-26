package cl.electricalc.app.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.app.ui.calculadoras.Formato
import cl.electricalc.datos.ric.EstadoVerificacion
import cl.electricalc.datos.ric.FuenteRic
import cl.electricalc.datos.ric.TipoVerificacion
import cl.electricalc.datos.ric.Verificacion

/** Colores del semáforo; se mantienen legibles en tema claro y oscuro. */
private val VERDE = Color(0xFF2E7D32)
private val AMBAR = Color(0xFFB26A00)

/** "RIC N°04, numeral 5.4" o "RIC N°04, tabla N°4.4 (numeral 6.2.2)". */
@Composable
fun citaRic(f: FuenteRic): String = if (f.tabla != null) {
    stringResource(R.string.cita_ric_tabla, f.pliego, f.tabla!!, f.numeral)
} else {
    stringResource(R.string.cita_ric, f.pliego, f.numeral)
}

@Composable
private fun nombreUso(uso: String): String = stringResource(
    when (uso) {
        "iluminacion" -> R.string.uso_iluminacion
        "enchufes" -> R.string.uso_enchufes
        "mixto" -> R.string.uso_mixto
        "subalimentador" -> R.string.uso_subalimentador
        else -> R.string.uso_alimentador
    },
)

@Composable
private fun FilaVerificacion(v: Verificacion, alAbrirPliego: (Int) -> Unit) {
    val (icono, color, estado) = when (v.estado) {
        EstadoVerificacion.CUMPLE -> Triple(Icons.Filled.CheckCircle, VERDE, R.string.estado_cumple)
        EstadoVerificacion.NO_CUMPLE -> Triple(Icons.Filled.Cancel, MaterialTheme.colorScheme.error, R.string.estado_no_cumple)
        EstadoVerificacion.REVISAR -> Triple(Icons.Filled.Error, AMBAR, R.string.estado_revisar)
    }
    val titulo = when (v.tipo) {
        TipoVerificacion.CAIDA_TRAMO -> stringResource(R.string.ver_caida_tramo)
        TipoVerificacion.CAIDA_TOTAL -> stringResource(R.string.ver_caida_total)
        TipoVerificacion.TENSION_CONTACTO -> stringResource(R.string.ver_tension_contacto)
        TipoVerificacion.AISLAMIENTO -> stringResource(R.string.ver_aislamiento)
        TipoVerificacion.SECCION_MINIMA -> v.uso?.let { stringResource(R.string.ver_seccion_uso, nombreUso(it.uso)) }
            ?: stringResource(R.string.ver_seccion)
    }
    val unidad = when (v.tipo) {
        TipoVerificacion.SECCION_MINIMA -> "mm²"
        TipoVerificacion.TENSION_CONTACTO -> "V"
        TipoVerificacion.AISLAMIENTO -> "MΩ"
        else -> "%"
    }
    val decimales = 2
    val detalle = when {
        v.limite == null || v.calculado == null -> stringResource(R.string.ver_seccion_sin_uso)
        else -> {
            val cumple = v.estado == EstadoVerificacion.CUMPLE
            // Caída: el valor debe quedar bajo el límite; sección: sobre la mínima.
            val signo = when (v.tipo) {
                TipoVerificacion.SECCION_MINIMA, TipoVerificacion.AISLAMIENTO -> if (cumple) "≥" else "<"
                else -> if (cumple) "≤" else ">"
            }
            "${Formato.fijo(v.calculado!!, decimales)} $unidad $signo ${Formato.numero(v.limite!!, 2)} $unidad"
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icono, contentDescription = stringResource(estado), tint = color, modifier = Modifier.size(24.dp))
        Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(stringResource(estado), color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
            }
            Text(detalle, style = MaterialTheme.typography.bodyMedium)
            if (v.tipo == TipoVerificacion.CAIDA_TOTAL) {
                Text(
                    stringResource(R.string.ver_caida_total_nota),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                citaRic(v.fuente),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { alAbrirPliego(v.fuente.pliego) },
            )
        }
    }
}

/**
 * Tarjeta "Verificación RIC" bajo el resultado: un semáforo por requisito, el valor frente al
 * límite y la cita exacta, que abre la página del pliego.
 */
@Composable
fun TarjetaVerificacion(verificaciones: List<Verificacion>, alAbrirPliego: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (verificaciones.isEmpty()) return
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.verificacion_ric), style = MaterialTheme.typography.labelLarge)
            verificaciones.forEachIndexed { i, v ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FilaVerificacion(v, alAbrirPliego)
            }
            if (verificaciones.any { it.fuente.revision.pendiente }) {
                Text(
                    stringResource(R.string.ver_en_revision),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
