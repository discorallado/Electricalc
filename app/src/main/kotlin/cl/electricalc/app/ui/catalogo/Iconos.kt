package cl.electricalc.app.ui.catalogo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Ícono de cada categoría de Calcular y Referencias. */
fun iconoCategoria(id: String): ImageVector = when (id) {
    "fundamentos" -> Icons.Filled.Bolt
    "conductores" -> Icons.Filled.Cable
    "protecciones" -> Icons.Filled.Shield
    "tierra" -> Icons.Filled.Public
    "energia" -> Icons.Filled.Speed
    "motores" -> Icons.Filled.Autorenew
    "sistemas" -> Icons.Filled.BatteryChargingFull
    "electronica" -> Icons.Filled.DeveloperBoard
    "ref_tablas" -> Icons.Filled.TableChart
    "ref_esquemas" -> Icons.Filled.AccountTree
    "ref_conectores" -> Icons.Filled.SettingsInputHdmi
    "ref_unidades" -> Icons.Filled.SwapHoriz
    "ref_formulario" -> Icons.Filled.Functions
    else -> Icons.Filled.Star
}

/** Dibujo de las herramientas nuevas según su tipo. */
private fun iconoTipo(tipo: String?): ImageVector? = when (tipo) {
    "verificacion" -> Icons.Filled.TaskAlt
    "checklist" -> Icons.Filled.Checklist
    "documento" -> Icons.Filled.Description
    "tabla" -> Icons.Filled.TableChart
    "esquema" -> Icons.Filled.AccountTree
    "asistente" -> Icons.Filled.Warning
    else -> null
}

/** Magnitudes que se muestran como símbolo dentro de un círculo en vez de un dibujo. */
private val simbolos = mapOf(
    "ico_intensita" to "I",
    "ico_tensione" to "V",
    "ico_potenza" to "P",
    "ico_va" to "S",
    "ico_var" to "Q",
    "ico_fattore_potenza" to "cosφ",
    "ico_resistenza" to "R",
    "ico_impedenza" to "Z",
    "ico_reattanza" to "X",
    "ico_impedenza_da_resistenza_e_reattanza" to "Z",
    "ico_caduta" to "ΔV",
    "ico_caduta_carichi_distribuiti" to "ΔV",
    "ico_icc" to "Icc",
    "ico_icct" to "Icc",
    "ico_icc_approssimata" to "Icc",
    "ico_k2s2" to "k²S²",
    "ico_corrente_impiego" to "Ib",
    "ico_corrente_neutro" to "In",
    "ico_phi" to "φ",
    "ico_joule" to "J",
    "ico_flc_motore" to "FLC",
    "ico_coppia" to "Nm",
    "ico_giri" to "rpm",
    "ico_scorrimento" to "s",
    "ico_rendimento" to "η",
    "ico_awg_mm2" to "AWG",
    "ico_swg" to "SWG",
    "ico_si" to "SI",
    "ico_unita" to "SI",
    "ico_ah_kwh" to "Ah",
    "ico_var_uf" to "µF",
    "ico_gauss_tesla" to "T",
    "ico_hz_rads" to "Hz",
    "ico_rpm" to "rpm",
    "ico_momento" to "Nm",
    "ico_conversione_byte" to "B",
    "ico_angolo" to "°",
    "ico_triangolo_stella" to "ΔY",
)

private fun dibujo(icono: String, categoria: String): ImageVector = when {
    "sezione" in icono || "portata" in icono || "conduttore" in icono || "cavo" in icono || "cavi" in icono -> Icons.Filled.Cable
    "lunghezza" in icono || "righello" in icono || "imperiale" in icono || "tubo" in icono -> Icons.Filled.Straighten
    "temperatura" in icono || "termocoppie" in icono || "sonde" in icono -> Icons.Filled.Thermostat
    "protezione" in icono || "fusibil" in icono || "spd" in icono || "curve" in icono || "terra" in icono -> Icons.Filled.Shield
    "atex" in icono || "grado" in icono || "classi" in icono -> Icons.Filled.Warning
    "batteria" in icono -> Icons.Filled.BatteryChargingFull
    "led" in icono -> Icons.Filled.Lightbulb
    "antenna" in icono -> Icons.Filled.SettingsInputAntenna
    "res_" in icono || "condensator" in icono || "induttor" in icono || "zener" in icono || "partitore" in icono ||
        "retma" in icono || "smd" in icono -> Icons.Filled.Palette
    "ev_" in icono -> Icons.Filled.ElectricCar
    "prese" in icono || "connettore" in icono || "cform" in icono -> Icons.Filled.Power
    "mondo" in icono || "sistemi" in icono -> Icons.Filled.Public
    "ethernet" in icono || "poe" in icono || "rj" in icono -> Icons.Filled.SettingsEthernet
    "usb" in icono || "lightning" in icono || "thunderbolt" in icono -> Icons.Filled.Usb
    "raspberry" in icono || "display" in icono || "io_link" in icono -> Icons.Filled.DeveloperBoard
    "motore" in icono || "condensatore_avviamento" in icono -> Icons.Filled.Autorenew
    "simboli" in icono || "acronimi" in icono || "ansi" in icono || "colori" in icono -> Icons.Filled.Category
    "rifasamento" in icono || "trasformatore" in icono -> Icons.Filled.ElectricalServices
    "energia" in icono || "pressione" in icono -> Icons.Filled.Speed
    else -> iconoCategoria(categoria)
}

/** Ícono de una fila del menú: símbolo de la magnitud o dibujo según el tema de la herramienta. */
@Composable
fun IconoItem(item: Herramienta, modifier: Modifier = Modifier) {
    val simbolo = item.icono?.let { simbolos[it] }
    Box(
        modifier = modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (simbolo != null) {
            Text(
                simbolo,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = if (simbolo.length <= 2) 16.sp else 11.sp,
                maxLines = 1,
            )
        } else {
            Icon(
                if (item.esFicha) Icons.Filled.Functions else iconoTipo(item.tipo) ?: dibujo(item.icono ?: item.id.lowercase(), item.categoria),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
