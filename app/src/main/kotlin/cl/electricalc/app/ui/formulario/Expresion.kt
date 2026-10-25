package cl.electricalc.app.ui.formulario

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/**
 * Fórmula como árbol: texto (con `_x` para subíndice y `^x` para superíndice), fracciones, raíces y
 * concatenaciones.
 */
sealed interface Expr {
    data class Texto(val texto: String) : Expr
    data class Fraccion(val numerador: Expr, val denominador: Expr) : Expr
    data class Raiz(val radicando: Expr) : Expr
    data class Fila(val partes: List<Expr>) : Expr
}

private fun aExpr(x: Any): Expr = when (x) {
    is Expr -> x
    is String -> Expr.Texto(x)
    else -> error("No es una expresión: $x")
}

fun e(vararg partes: Any): Expr = if (partes.size == 1) aExpr(partes[0]) else Expr.Fila(partes.map(::aExpr))
fun frac(numerador: Any, denominador: Any): Expr = Expr.Fraccion(aExpr(numerador), aExpr(denominador))
fun raiz(radicando: Any): Expr = Expr.Raiz(aExpr(radicando))

/** Tarjeta de la ficha: un tipo de corriente y sus fórmulas. */
class TarjetaFicha(@StringRes val titulo: Int, val formulas: List<Expr>)

/** Entrada de la leyenda: símbolo (con la misma notación de [Expr.Texto]), significado y unidad. */
class Simbolo(val simbolo: String, @StringRes val descripcion: Int, val unidad: String? = null)

class Ficha(val tarjetas: List<TarjetaFicha>, val leyenda: List<Simbolo>, @StringRes val notas: List<Int> = emptyList())

/** Convierte `U_0`, `I^2` o `R_20` en texto con subíndices y superíndices. */
fun textoConIndices(texto: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < texto.length) {
        val c = texto[i]
        if ((c == '_' || c == '^') && i + 1 < texto.length) {
            var fin = i + 1
            while (fin < texto.length && texto[fin].isLetterOrDigit()) fin++
            val desplazamiento = if (c == '_') BaselineShift.Subscript else BaselineShift.Superscript
            withStyle(SpanStyle(baselineShift = desplazamiento, fontSize = 0.7.em)) { append(texto.substring(i + 1, fin)) }
            i = fin
        } else {
            append(c)
            i++
        }
    }
}

@Composable
fun VistaFormula(expr: Expr, estilo: TextStyle, modifier: Modifier = Modifier) {
    when (expr) {
        is Expr.Texto -> Text(textoConIndices(expr.texto), style = estilo, modifier = modifier)
        is Expr.Fila -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            expr.partes.forEach { VistaFormula(it, estilo) }
        }
        is Expr.Fraccion -> Column(
            modifier.width(IntrinsicSize.Max).padding(horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VistaFormula(expr.numerador, estilo)
            HorizontalDivider(Modifier.fillMaxWidth().padding(vertical = 2.dp), thickness = 1.dp, color = LocalContentColor.current)
            VistaFormula(expr.denominador, estilo)
        }
        is Expr.Raiz -> {
            val color = LocalContentColor.current
            Row(modifier.height(IntrinsicSize.Min).padding(horizontal = 2.dp)) {
                // Signo radical dibujado: trazo corto, bajada y subida hasta la barra superior.
                Canvas(Modifier.width(10.dp).fillMaxHeight()) {
                    val trazo = 1.dp.toPx()
                    val p = Path().apply {
                        moveTo(0f, size.height * 0.62f)
                        lineTo(size.width * 0.3f, size.height * 0.55f)
                        lineTo(size.width * 0.55f, size.height)
                        lineTo(size.width, trazo / 2)
                    }
                    drawPath(p, color, style = Stroke(width = trazo))
                }
                Box(
                    Modifier
                        .drawBehind { drawLine(color, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx()) }
                        .padding(top = 3.dp, start = 1.dp, end = 2.dp),
                ) {
                    VistaFormula(expr.radicando, estilo)
                }
            }
        }
    }
}
