package cl.electricalc.app.ui.catalogo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Una herramienta del catálogo v2: las 221 que vienen de la app de referencia ([nueva] = false) y
 * las propuestas para Chile ([nueva] = true). [id] es la clave estable en toda la app.
 */
data class Herramienta(
    val id: String,
    val tituloEs: String,
    val tituloEn: String?,
    val categoria: String,
    /** Pliegos RIC relacionados. */
    val ric: List<Int>,
    /** "alta" si el tema es el objeto del pliego; "media" si hay que confirmar el numeral en el PDF. */
    val ricConfianza: String?,
    val etapa: String?,
    /** Ficha del formulario asociada (id de otra herramienta). */
    val formula: String?,
    val nueva: Boolean,
    /** Solo en las nuevas: calculadora, verificacion, asistente, esquema, documento, checklist o tabla. */
    val tipo: String?,
    val prioridad: Int?,
    val descripcion: String?,
    /** Tablas RIC que necesita y que todavía no están transcritas. */
    val datosRic: List<String>,
    /** Ícono de la app de referencia, para elegir el dibujo. */
    val icono: String?,
    val sinonimos: List<String>,
) {
    val titulo: String get() = if (Locale.getDefault().language == "es" || tituloEn == null) tituloEs else tituloEn
    val esFicha: Boolean get() = categoria == Categorias.FORMULARIO
    val ricSeguro: Boolean get() = ricConfianza != "media"
}

data class Pliego(
    val numero: Int,
    val titulo: String,
    val existentes: List<String>,
    val nuevas: List<String>,
) {
    val codigo: String get() = "RIC N°%02d".format(numero)
}

object Categorias {
    const val FORMULARIO = "ref_formulario"

    /** Categorías de la pestaña Calcular, en orden. */
    val CALCULAR = listOf(
        "fundamentos", "conductores", "protecciones", "tierra", "energia", "motores", "sistemas", "electronica",
    )

    /** Secciones de la pestaña Referencias, en orden. */
    val REFERENCIAS = listOf("ref_tablas", "ref_esquemas", "ref_conectores", "ref_unidades", FORMULARIO)
}

/** Etapas del flujo de un proyecto (campo `etapa`), en orden. */
val ETAPAS = listOf("cargas", "empalme", "tableros", "alimentadores", "protecciones", "tierra", "sistemas", "documentacion")

class Catalogo(
    val herramientas: List<Herramienta>,
    val pliegos: List<Pliego>,
    /** Nombre en español de cada categoría (campo `categorias` del v2), para el buscador. */
    val nombresCategoria: Map<String, String> = emptyMap(),
) {
    private val porId = herramientas.associateBy { it.id }

    fun item(id: String): Herramienta? = porId[id]

    fun pliego(numero: Int): Pliego? = pliegos.firstOrNull { it.numero == numero }

    /** Herramientas de una categoría: primero las que tienen verificación RIC, luego el resto. */
    fun categoria(id: String): List<Herramienta> =
        herramientas.filter { it.categoria == id }.sortedByDescending { it.datosRic.isNotEmpty() }

    fun etapa(id: String): List<Herramienta> =
        // Primero las herramientas pensadas para el flujo (nuevas, por prioridad), luego las heredadas del APK.
        herramientas.filter { it.etapa == id && !it.esFicha }.sortedWith(compareBy({ !it.nueva }, { it.prioridad ?: 9 }))

    fun ids(lista: List<String>): List<Herramienta> = lista.mapNotNull { porId[it] }

    /** Favoritos en el orden del catálogo. */
    fun favoritos(ids: Set<String>): List<Herramienta> = herramientas.filter { it.id in ids }

    val buscador by lazy { Buscador(herramientas, pliegos, nombresCategoria) }

    companion object {
        fun cargar(contexto: Context): Catalogo {
            fun leer(nombre: String) = contexto.assets.open(nombre).bufferedReader().use { it.readText() }
            return desdeJson(leer("catalogo-v2.json"), leer("catalogo.json"), leer("sinonimos.json"))
        }

        /** Arma el catálogo desde los tres JSON de `spec/`. Separado de [cargar] para poder probarlo. */
        fun desdeJson(v2: String, v1: String, sinonimos: String): Catalogo {
            // Del catálogo v1 salen el título en inglés y el ícono, por fragment ("Fragment" + id).
            val referencia = mutableMapOf<String, JSONObject>()
            JSONArray(v1).let { secciones ->
                for (i in 0 until secciones.length()) {
                    val items = secciones.getJSONObject(i).getJSONArray("items")
                    for (j in 0 until items.length()) items.getJSONObject(j).let { referencia[it.getString("fragment")] = it }
                }
            }
            val extra = JSONObject(sinonimos)
            val raiz = JSONObject(v2)

            fun JSONObject.texto(clave: String) = optString(clave).takeIf { has(clave) && !isNull(clave) && it.isNotEmpty() }
            fun JSONObject.numeros(clave: String) = optJSONArray(clave)?.let { a -> (0 until a.length()).map { a.getInt(it) } }.orEmpty()
            fun JSONObject.textos(clave: String) = optJSONArray(clave)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()

            fun herramienta(o: JSONObject, nueva: Boolean): Herramienta {
                val id = o.getString("id")
                val v1 = referencia["Fragment$id"]
                return Herramienta(
                    id = id,
                    tituloEs = limpiar(o.getString("titulo")),
                    tituloEn = v1?.texto("titulo_en")?.let(::limpiar),
                    categoria = o.getString("categoria"),
                    ric = o.numeros("ric"),
                    ricConfianza = o.texto("ric_confianza"),
                    etapa = o.texto("etapa"),
                    formula = o.texto("formula"),
                    nueva = nueva,
                    tipo = o.texto("tipo"),
                    prioridad = if (o.has("prioridad")) o.getInt("prioridad") else null,
                    descripcion = o.texto("descripcion"),
                    datosRic = o.textos("datos_ric"),
                    icono = v1?.texto("icono"),
                    sinonimos = extra.textos(id),
                )
            }

            val existentes = raiz.getJSONArray("existentes")
            val nuevas = raiz.getJSONArray("nuevas")
            val herramientas = (0 until existentes.length()).map { herramienta(existentes.getJSONObject(it), false) } +
                (0 until nuevas.length()).map { herramienta(nuevas.getJSONObject(it), true) }
            val pliegos = raiz.getJSONArray("pliegos").let { a ->
                (0 until a.length()).map { i ->
                    val p = a.getJSONObject(i)
                    Pliego(p.getInt("numero"), p.getString("titulo"), p.textos("herramientas_existentes"), p.textos("herramientas_nuevas"))
                }
            }
            val categorias = raiz.getJSONObject("categorias").let { c -> c.keys().asSequence().associateWith { c.getString(it) } }
            return Catalogo(herramientas, pliegos, categorias)
        }

        /** Algunos títulos vienen con comillas o con dos puntos finales (etiquetas de campo). */
        private fun limpiar(t: String) = t.trim().removeSurrounding("\"").trimEnd(':').trim()
    }
}
