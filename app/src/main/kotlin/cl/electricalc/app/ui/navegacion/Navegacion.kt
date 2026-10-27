package cl.electricalc.app.ui.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.electricalc.app.datos.LocalPreferencias
import cl.electricalc.app.R
import cl.electricalc.app.datos.Proyecto
import cl.electricalc.app.ui.proyecto.PantallaProyecto
import androidx.compose.ui.platform.LocalContext
import cl.electricalc.app.ui.calculadoras.PantallaCalculadora
import cl.electricalc.app.ui.calculadoras.Registro
import cl.electricalc.app.ui.catalogo.Catalogo
import cl.electricalc.app.ui.catalogo.Herramienta
import cl.electricalc.app.ui.componentes.PantallaConBarra
import cl.electricalc.app.ui.componentes.HERRAMIENTAS_PROYECTO
import cl.electricalc.app.ui.componentes.nombreCategoria
import cl.electricalc.app.ui.destinos.Accesos
import cl.electricalc.app.ui.destinos.ListaHerramientas
import cl.electricalc.app.ui.destinos.PantallaPendiente
import cl.electricalc.app.ui.destinos.PantallaPliego
import cl.electricalc.app.ui.formulario.Fichas
import cl.electricalc.app.ui.formulario.PantallaFicha
import cl.electricalc.app.ui.guia.PantallaGuia
import cl.electricalc.app.ui.ric.PantallaTablaRic
import cl.electricalc.app.ui.ric.TablasRic
import cl.electricalc.app.ui.referencias.PantallaReferencia
import cl.electricalc.app.ui.referencias.Referencias

/** Rutas: cada herramienta del catálogo se abre por su id del catálogo v2. */
object Rutas {
    const val PRINCIPAL = "principal"
    const val AJUSTES = "ajustes"
    const val ACERCA = "acerca"
    const val GUIA_USO = "guia_uso"
    fun herramienta(id: String) = "herramienta/$id"
    fun ficha(id: String) = "ficha/$id"
    fun guia(id: String) = "guia/$id"
    fun pliego(n: Int) = "pliego/$n"
    fun categoria(id: String) = "categoria/$id"
    fun acceso(id: String) = "acceso/$id"
    fun tablaRic(clave: String) = "tabla_ric/$clave"
}

private fun NavHostController.volver() {
    if (previousBackStackEntry != null) popBackStack()
}

@Composable
fun NavegacionApp(catalogo: Catalogo) {
    val nav = rememberNavController()
    val preferencias = LocalPreferencias.current
    val contexto = LocalContext.current

    val abrir = { h: Herramienta ->
        preferencias.registrarReciente(h.id)
        if (h.id in HERRAMIENTAS_PROYECTO) {
            val p = Proyecto(nombre = contexto.getString(R.string.proyecto_sin_nombre))
            preferencias.guardarProyecto(p)
            nav.navigate("proyecto/${p.id}")
        } else {
            nav.navigate(if (h.esFicha) Rutas.ficha(h.id) else Rutas.herramienta(h.id))
    }
    }
    val abrirPliego = { n: Int -> nav.navigate(Rutas.pliego(n)) }
    val alMantener = { h: Herramienta -> preferencias.alternarFavorito(h.id) }
    val acciones = Acciones(
        abrir = abrir,
        abrirPliego = abrirPliego,
        abrirCategoria = { nav.navigate(Rutas.categoria(it)) },
        abrirAcceso = { nav.navigate(Rutas.acceso(it)) },
        abrirAjustes = { nav.navigate(Rutas.AJUSTES) },
        abrirAcerca = { nav.navigate(Rutas.ACERCA) },
        abrirGuiaUso = { nav.navigate(Rutas.GUIA_USO) },
        abrirProyecto = { nav.navigate("proyecto/$it") },
        nuevoProyecto = {
            val p = Proyecto(nombre = contexto.getString(R.string.proyecto_sin_nombre))
            preferencias.guardarProyecto(p)
            nav.navigate("proyecto/${p.id}")
        },
    )

    NavHost(navController = nav, startDestination = Rutas.PRINCIPAL) {
        composable(Rutas.PRINCIPAL) { PantallaPrincipal(catalogo, acciones) }

        composable("herramienta/{id}") { entrada ->
            val id = entrada.arguments?.getString("id").orEmpty()
            val item = catalogo.item(id)
            val calculadora = Registro.calculadora(id)
            when {
                item != null && TablasRic.claveDeHerramienta(id) != null -> PantallaTablaRic(
                    TablasRic.porClave.getValue(TablasRic.claveDeHerramienta(id)!!), nav::volver,
                )
                item != null && Referencias.existe(id) -> PantallaReferencia(item.titulo, id, nav::volver)
                item != null && calculadora != null -> PantallaCalculadora(
                    calculadora = calculadora,
                    item = item,
                    alVolver = nav::volver,
                    alAbrirFicha = { nav.navigate(Rutas.ficha(it)) },
                    alAbrirGuia = { nav.navigate(Rutas.guia(id)) },
                    alAbrirPliego = abrirPliego,
                )
                item != null -> PantallaPendiente(item, nav::volver, abrirPliego)
            }
        }
        composable("ficha/{id}") { entrada ->
            val id = entrada.arguments?.getString("id").orEmpty()
            val item = catalogo.item(id)
            if (Fichas.existe(id) || item == null) {
                PantallaFicha(id, item?.titulo.orEmpty(), nav::volver)
            } else {
                PantallaPendiente(item, nav::volver, abrirPliego)
            }
        }
        composable("guia/{id}") { entrada ->
            val id = entrada.arguments?.getString("id").orEmpty()
            val calculadora = Registro.calculadora(id)
            if (calculadora != null) PantallaGuia(calculadora, catalogo.item(id)?.titulo.orEmpty(), nav::volver)
        }
        composable("pliego/{n}") { entrada ->
            val pliego = entrada.arguments?.getString("n")?.toIntOrNull()?.let(catalogo::pliego)
            if (pliego != null) {
                PantallaPliego(catalogo, pliego, nav::volver, abrir, alMantener) { nav.navigate(Rutas.tablaRic(it)) }
            }
        }
        composable("categoria/{id}") { entrada ->
            val id = entrada.arguments?.getString("id").orEmpty()
            PantallaConBarra(stringResource(nombreCategoria(id)), nav::volver) { m ->
                ListaHerramientas(catalogo.categoria(id), m, abrir, alMantener)
            }
        }
        composable("acceso/{id}") { entrada ->
            val acceso = Accesos.acceso(entrada.arguments?.getString("id").orEmpty())
            if (acceso != null) {
                PantallaConBarra(stringResource(acceso.titulo), nav::volver) { m ->
                    ListaHerramientas(catalogo.ids(acceso.ids), m, abrir, alMantener)
                }
            }
        }
        composable("tabla_ric/{clave}") { entrada ->
            TablasRic.porClave[entrada.arguments?.getString("clave")]?.let { PantallaTablaRic(it, nav::volver) }
        }
        composable("proyecto/{id}") { entrada ->
            PantallaProyecto(entrada.arguments?.getString("id").orEmpty(), nav::volver)
        }
        composable(Rutas.AJUSTES) { PantallaAjustes(nav::volver) }
        composable(Rutas.ACERCA) { PantallaAcerca(nav::volver) }
        composable(Rutas.GUIA_USO) { PantallaGuiaUso(nav::volver) }
    }
}
