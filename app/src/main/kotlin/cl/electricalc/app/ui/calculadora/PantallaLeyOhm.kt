package cl.electricalc.app.ui.calculadora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.electricalc.app.R
import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.ohm.LeyOhm
import cl.electricalc.calc.ohm.Magnitud
import java.text.NumberFormat

/** Salida elegible: una magnitud o el cosφ (null). */
private val salidas: List<Magnitud?> = Magnitud.entries + listOf(null)

@Composable
private fun nombreSistema(s: SistemaElectrico) = stringResource(
    when (s) {
        SistemaElectrico.CONTINUA -> R.string.sistema_continua
        SistemaElectrico.MONOFASICA -> R.string.sistema_monofasica
        SistemaElectrico.BIFASICA -> R.string.sistema_bifasica
        SistemaElectrico.TRIFASICA -> R.string.sistema_trifasica
    },
)

@Composable
private fun nombreMagnitud(m: Magnitud?) = stringResource(
    when (m) {
        Magnitud.TENSION -> R.string.mag_tension
        Magnitud.CORRIENTE -> R.string.mag_corriente
        Magnitud.RESISTENCIA -> R.string.mag_resistencia
        Magnitud.IMPEDANCIA -> R.string.mag_impedancia
        Magnitud.POTENCIA_ACTIVA -> R.string.mag_potencia_activa
        Magnitud.POTENCIA_REACTIVA -> R.string.mag_potencia_reactiva
        Magnitud.POTENCIA_APARENTE -> R.string.mag_potencia_aparente
        null -> R.string.mag_cos_phi
    },
)

private fun mensajeError(motivo: MotivoError): Int = when (motivo) {
    MotivoError.DATOS_INSUFICIENTES -> R.string.err_datos_insuficientes
    MotivoError.NO_APLICA_EN_CONTINUA -> R.string.err_no_aplica_continua
    MotivoError.COS_PHI_NO_VALIDO -> R.string.err_cos_phi
    else -> R.string.err_valor_no_valido
}

private sealed interface Resultado {
    data class Valor(val valor: Double) : Resultado
    data class Error(val mensaje: Int) : Resultado
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLeyOhm(alVolver: () -> Unit) {
    var sistema by remember { mutableStateOf(SistemaElectrico.MONOFASICA) }
    var salida by remember { mutableStateOf<Magnitud?>(Magnitud.CORRIENTE) }
    val textos = remember { mutableStateMapOf<Magnitud, String>() }
    var cosPhi by remember { mutableStateOf("1") }
    var resultado by remember { mutableStateOf<Resultado?>(null) }

    fun calcular() {
        val conocidas = Magnitud.entries
            .filter { it != salida }
            .mapNotNull { m -> leerNumero(textos[m].orEmpty())?.let { m to it } }
            .toMap()
        val datos = LeyOhm.Datos(sistema, conocidas, leerNumero(cosPhi) ?: 1.0)
        resultado = try {
            val s = salida
            Resultado.Valor(if (s == null) LeyOhm.calcularCosPhi(datos) else LeyOhm.calcular(datos, s))
        } catch (e: ErrorCalculo) {
            Resultado.Error(mensajeError(e.motivo))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calc_ley_ohm)) },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                },
            )
        },
    ) { relleno ->
        Column(
            modifier = Modifier
                .padding(relleno)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Selector(
                etiqueta = stringResource(R.string.sistema),
                opciones = SistemaElectrico.entries,
                seleccion = sistema,
                texto = { nombreSistema(it) },
                alElegir = { sistema = it; resultado = null },
            )
            Selector(
                etiqueta = stringResource(R.string.dato_a_calcular),
                opciones = salidas,
                seleccion = salida,
                texto = { nombreMagnitud(it) },
                alElegir = { salida = it; resultado = null },
            )
            Text(stringResource(R.string.datos_conocidos), style = MaterialTheme.typography.titleSmall)
            Magnitud.entries.filter { it != salida }.forEach { m ->
                val aplica = sistema.esAlterna || m != Magnitud.POTENCIA_REACTIVA
                if (aplica) {
                    CampoNumerico(nombreMagnitud(m), textos[m].orEmpty(), { textos[m] = it })
                }
            }
            if (sistema.esAlterna && salida != null) {
                CampoNumerico(stringResource(R.string.cos_phi), cosPhi, { cosPhi = it })
            }
            Button(onClick = ::calcular, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.calcular))
            }
            when (val r = resultado) {
                is Resultado.Valor -> Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.resultado), style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${nombreMagnitud(salida)}: ${formatear(r.valor)}",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
                is Resultado.Error -> Text(stringResource(r.mensaje), color = MaterialTheme.colorScheme.error)
                null -> Unit
            }
        }
    }
}

private fun formatear(valor: Double): String =
    NumberFormat.getNumberInstance().apply { maximumFractionDigits = 4 }.format(valor)
