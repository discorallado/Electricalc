package cl.electricalc.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.electricalc.app.ui.calculadora.PantallaLeyOhm
import cl.electricalc.app.ui.inicio.PantallaInicio

object Rutas {
    const val INICIO = "inicio"
    const val LEY_OHM = "ley_ohm"
}

@Composable
fun NavegacionApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Rutas.INICIO) {
        composable(Rutas.INICIO) {
            PantallaInicio(alAbrir = { ruta -> nav.navigate(ruta) })
        }
        composable(Rutas.LEY_OHM) {
            PantallaLeyOhm(alVolver = { nav.popBackStack() })
        }
    }
}
