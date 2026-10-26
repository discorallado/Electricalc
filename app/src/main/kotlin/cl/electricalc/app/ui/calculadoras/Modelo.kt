package cl.electricalc.app.ui.calculadoras

import androidx.annotation.StringRes
import cl.electricalc.datos.ric.Verificacion
import cl.electricalc.app.datos.Preferencias
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.ohm.Magnitud

/**
 * Unidad de un campo. [aBase] lleva el valor a la unidad base del campo (V, A, Ω, W, m, °C, cosφ).
 * En los campos de potencia la unidad decide además qué potencia es ([magnitud]).
 */
class Unidad(
    val simbolo: String,
    val magnitud: Magnitud? = null,
    val aBase: (Double) -> Double = { it },
)

/** Campo del formulario. La [ayuda] se muestra con el ícono ⓘ y en la guía de la pantalla. */
sealed interface Campo {
    val id: String
    @get:StringRes val etiqueta: Int
    val ayuda: List<Int>
}

/** Valor numérico con selector de unidad al lado. */
class CampoNumerico(
    override val id: String,
    @StringRes override val etiqueta: Int,
    override val ayuda: List<Int>,
    /** Unidades disponibles según el tipo de corriente; la primera de [porDefecto] que exista se elige al inicio. */
    val unidades: (SistemaElectrico) -> List<Unidad>,
    val porDefecto: (Preferencias) -> String,
    /** Magnitud fija del campo; si es null la da la unidad elegida (campos de potencia o carga). */
    val magnitud: Magnitud? = null,
    val hint: (SistemaElectrico) -> String = { "" },
    /** Texto con que se rellena el campo al abrir la pantalla o cambiar el tipo de corriente. */
    val inicial: ((SistemaElectrico, Preferencias) -> String)? = null,
) : Campo

/** Lista de opciones fijas (material, tipo de cable, conductores en paralelo). */
class CampoOpciones(
    override val id: String,
    @StringRes override val etiqueta: Int,
    override val ayuda: List<Int>,
    val opciones: List<TextoOpcion>,
    val porDefecto: Int = 0,
) : Campo

/** Sección del conductor: medida normalizada y unidad (mm² o AWG/kcmil). */
class CampoSeccion(
    override val id: String,
    @StringRes override val etiqueta: Int,
    override val ayuda: List<Int>,
) : Campo

/** Texto de una opción: recurso traducible o literal (números, calibres). */
sealed interface TextoOpcion {
    data class Recurso(@StringRes val id: Int) : TextoOpcion
    data class Literal(val texto: String) : TextoOpcion
}

/** Combinación de datos conocidos ("Tensión y potencia"), o un método con nombre propio ("Wenner"). */
class Variante(val campos: List<String>, @StringRes val nombre: Int? = null)

/** Lo que el formulario entrega al cálculo, ya convertido a unidades base. */
class Entrada(
    val sistema: SistemaElectrico,
    val variante: Variante?,
    private val valores: Map<String, Double>,
    private val unidades: Map<String, Unidad>,
    private val opciones: Map<String, Int>,
) {
    fun valor(id: String): Double = valores[id] ?: throw FaltanDatos()
    /** Si el campo estaba visible (y por lo tanto se leyó). */
    fun tiene(id: String): Boolean = id in valores
    fun unidad(id: String): Unidad = unidades.getValue(id)
    fun opcion(id: String): Int = opciones.getValue(id)
}

/** Algún campo visible está vacío o no es un número. */
class FaltanDatos : Exception()

/** Una línea de la tarjeta de resultados: etiqueta, valor y unidad ya formateados. */
data class LineaResultado(@StringRes val etiqueta: Int, val valor: String, val principal: Boolean = true)

/** Explicación de la pantalla: párrafos de introducción; los parámetros salen de los campos. */
class Guia(val intro: List<Int>)

/**
 * Definición declarativa de una pantalla de cálculo. El título y la ficha de fórmulas salen del
 * catálogo ([id] es la clave del ítem); la pantalla se dibuja siempre con los mismos componentes.
 */
class Calculadora(
    val id: String,
    val sistemas: List<SistemaElectrico>,
    /** Todos los campos posibles, en el orden en que se muestran. */
    val campos: List<Campo>,
    /** Combinaciones de datos conocidos; vacío si la pantalla no tiene selector. */
    val variantes: (SistemaElectrico) -> List<Variante> = { emptyList() },
    /** Si el campo se muestra con el tipo de corriente, la variante y las unidades elegidas. */
    val visible: (campo: Campo, sistema: SistemaElectrico, variante: Variante?, unidades: Map<String, Unidad>) -> Boolean,
    val guia: Guia,
    val calcular: (Entrada) -> List<LineaResultado>,
    /** Verificaciones contra el RIC que se muestran bajo el resultado (semáforo). */
    val verificar: (Entrada) -> List<Verificacion> = { emptyList() },
)
