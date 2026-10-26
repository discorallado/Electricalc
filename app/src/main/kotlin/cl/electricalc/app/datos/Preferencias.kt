package cl.electricalc.app.datos

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import cl.electricalc.calc.comun.SistemaElectrico
import java.text.DecimalFormatSymbols

/**
 * Ajustes y favoritos guardados en SharedPreferences. Cada valor es estado de Compose, así que la UI
 * se recompone sola al cambiarlo.
 */
class Preferencias(contexto: Context) {
    private val sp = contexto.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    /**
     * Id (catálogo v2) de cada herramienta favorita. Los guardados con el id del catálogo v1
     * ("FragmentCalcoloCaduta") se pasan al v2 ("CalcoloCaduta") al leerlos.
     */
    var favoritos by mutableStateOf(sp.getStringSet(FAVORITOS, emptySet())!!.map { it.removePrefix("Fragment") }.toSet())
        private set

    fun esFavorito(id: String) = id in favoritos

    fun alternarFavorito(id: String) {
        favoritos = if (id in favoritos) favoritos - id else favoritos + id
        sp.edit { putStringSet(FAVORITOS, favoritos) }
    }

    /** Proyectos guardados, el más reciente primero. */
    var proyectos by mutableStateOf(
        runCatching {
            val a = org.json.JSONArray(sp.getString(PROYECTOS, "[]"))
            (0 until a.length()).map { Proyecto.deJson(a.getJSONObject(it)) }
        }.getOrDefault(emptyList()),
    )
        private set

    fun proyecto(id: String): Proyecto? = proyectos.firstOrNull { it.id == id }

    fun guardarProyecto(p: Proyecto) {
        proyectos = listOf(p) + proyectos.filter { it.id != p.id }
        sp.edit { putString(PROYECTOS, org.json.JSONArray(proyectos.map { it.aJson() }).toString()) }
    }

    fun eliminarProyecto(id: String) {
        proyectos = proyectos.filter { it.id != id }
        sp.edit { putString(PROYECTOS, org.json.JSONArray(proyectos.map { it.aJson() }).toString()) }
    }

    /** Últimas herramientas abiertas, la más reciente primero. */
    var recientes by mutableStateOf(sp.getString(RECIENTES, "").orEmpty().split(',').filter { it.isNotBlank() })
        private set

    fun registrarReciente(id: String) {
        recientes = (listOf(id) + recientes.filter { it != id }).take(MAX_RECIENTES)
        sp.edit { putString(RECIENTES, recientes.joinToString(",")) }
    }

    /** Norma que se propone primero en las calculadoras que dependen de ella. */
    var norma by mutableStateOf(runCatching { Norma.valueOf(sp.getString(NORMA, null)!!) }.getOrDefault(Norma.RIC))
        private set

    fun fijarNorma(valor: Norma) {
        norma = valor
        sp.edit { putString(NORMA, valor.name) }
    }

    private val tensiones = SistemaElectrico.entries.associateWith { s ->
        mutableStateOf(sp.getString(claveTension(s), TENSION_POR_DEFECTO[s]) ?: "")
    }

    /** Tensión con la que se rellena el campo al elegir el tipo de corriente ("" = no rellenar). */
    fun tensionPorDefecto(sistema: SistemaElectrico): String = tensiones.getValue(sistema).value

    fun fijarTensionPorDefecto(sistema: SistemaElectrico, valor: String) {
        tensiones.getValue(sistema).value = valor
        sp.edit { putString(claveTension(sistema), valor) }
    }

    // Sin valor guardado se muestra 0,9 con el separador decimal del idioma.
    var cosPhiPorDefecto by mutableStateOf(sp.getString(COS_PHI, null) ?: "0${DecimalFormatSymbols.getInstance().decimalSeparator}9")
        private set

    fun fijarCosPhiPorDefecto(valor: String) {
        cosPhiPorDefecto = valor
        sp.edit { putString(COS_PHI, valor) }
    }

    var longitudEnPies by mutableStateOf(sp.getBoolean(PIES, false))
        private set

    fun fijarLongitudEnPies(valor: Boolean) {
        longitudEnPies = valor
        sp.edit { putBoolean(PIES, valor) }
    }

    var seccionEnAwg by mutableStateOf(sp.getBoolean(AWG, false))
        private set

    fun fijarSeccionEnAwg(valor: Boolean) {
        seccionEnAwg = valor
        sp.edit { putBoolean(AWG, valor) }
    }

    var temperaturaEnFahrenheit by mutableStateOf(sp.getBoolean(FAHRENHEIT, false))
        private set

    fun fijarTemperaturaEnFahrenheit(valor: Boolean) {
        temperaturaEnFahrenheit = valor
        sp.edit { putBoolean(FAHRENHEIT, valor) }
    }

    private companion object {
        const val FAVORITOS = "favoritos"
        const val RECIENTES = "recientes"
        const val PROYECTOS = "proyectos"
        const val MAX_RECIENTES = 8
        const val NORMA = "norma"
        const val COS_PHI = "cos_phi_por_defecto"
        const val PIES = "longitud_en_pies"
        const val AWG = "seccion_en_awg"
        const val FAHRENHEIT = "temperatura_en_fahrenheit"

        fun claveTension(s: SistemaElectrico) = "tension_por_defecto_${s.name.lowercase()}"

        /** Valores habituales en Chile (220 V fase-neutro, 380 V entre fases). */
        val TENSION_POR_DEFECTO = mapOf(
            SistemaElectrico.CONTINUA to "",
            SistemaElectrico.MONOFASICA to "220",
            SistemaElectrico.BIFASICA to "380",
            SistemaElectrico.TRIFASICA to "380",
        )
    }
}

/** Norma de diseño por defecto. */
enum class Norma { RIC, IEC, NEC }

val LocalPreferencias = staticCompositionLocalOf<Preferencias> { error("Preferencias no provistas") }
