package cl.electricalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.electricalc.app.ui.NavegacionApp
import cl.electricalc.app.ui.tema.TemaElectricalc

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaElectricalc {
                NavegacionApp()
            }
        }
    }
}
