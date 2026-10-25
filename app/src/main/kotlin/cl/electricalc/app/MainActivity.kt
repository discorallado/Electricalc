package cl.electricalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import cl.electricalc.app.datos.LocalPreferencias
import cl.electricalc.app.datos.Preferencias
import cl.electricalc.app.ui.catalogo.Catalogo
import cl.electricalc.app.ui.navegacion.NavegacionApp
import cl.electricalc.app.ui.tema.TemaElectricalc

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferencias = Preferencias(applicationContext)
        val catalogo = Catalogo.cargar(applicationContext)
        setContent {
            TemaElectricalc {
                CompositionLocalProvider(LocalPreferencias provides remember { preferencias }) {
                    NavegacionApp(catalogo)
                }
            }
        }
    }
}
