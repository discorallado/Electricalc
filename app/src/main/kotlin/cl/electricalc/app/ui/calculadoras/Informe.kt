package cl.electricalc.app.ui.calculadoras

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import cl.electricalc.app.R
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Resumen para compartir como texto o PDF: secciones con filas "dato: valor" o líneas sueltas. */
class Informe(val titulo: String, val secciones: List<Pair<String, List<Pair<String, String>>>>) {

    /** Informe de una calculadora: datos y resultados. */
    constructor(contexto: Context, titulo: String, datos: List<Pair<String, String>>, resultados: List<Pair<String, String>>) :
        this(titulo, listOf(contexto.getString(R.string.datos) to datos, contexto.getString(R.string.resultado) to resultados))

    fun comoTexto(contexto: Context): String = buildString {
        appendLine(titulo)
        secciones.forEach { (nombre, filas) ->
            appendLine()
            appendLine(nombre)
            filas.forEach { (k, v) -> appendLine(if (v.isEmpty()) "• $k" else "• $k: $v") }
        }
        appendLine()
        append(contexto.getString(R.string.generado_con, contexto.getString(R.string.app_name)))
    }

    fun compartirTexto(contexto: Context) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            putExtra(Intent.EXTRA_TEXT, comoTexto(contexto))
        }
        contexto.startActivity(Intent.createChooser(intent, contexto.getString(R.string.compartir)))
    }

    /** Dibuja el informe en páginas A4 (con salto de página) y lo comparte. */
    fun compartirPdf(contexto: Context) {
        val documento = PdfDocument()
        val titular = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val seccion = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val normal = Paint().apply { textSize = 11f; isAntiAlias = true }
        val tenue = Paint().apply { textSize = 10f; color = 0xFF666666.toInt(); isAntiAlias = true }
        var numero = 0
        var pagina: PdfDocument.Page? = null
        var y = 0f

        fun nuevaPagina() {
            pagina?.let {
                it.canvas.drawText(contexto.getString(R.string.generado_con, contexto.getString(R.string.app_name)), 50f, 810f, tenue)
                documento.finishPage(it)
            }
            numero++
            pagina = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, numero).create())
            y = 60f
        }

        fun espacio(alto: Float) {
            if (y + alto > 780f) nuevaPagina()
            y += alto
        }

        /** Corta un texto largo en líneas que quepan en [ancho] puntos. */
        fun lineas(texto: String, paint: Paint, ancho: Float): List<String> {
            val salida = mutableListOf<String>()
            var actual = ""
            for (palabra in texto.split(' ')) {
                val prueba = if (actual.isEmpty()) palabra else "$actual $palabra"
                if (paint.measureText(prueba) > ancho && actual.isNotEmpty()) {
                    salida += actual
                    actual = palabra
                } else {
                    actual = prueba
                }
            }
            if (actual.isNotEmpty()) salida += actual
            return salida
        }

        nuevaPagina()
        lineas(titulo, titular, 495f).forEach { espacio(22f); pagina!!.canvas.drawText(it, 50f, y, titular) }
        espacio(16f)
        pagina!!.canvas.drawText(DateFormat.getDateTimeInstance().format(Date()), 50f, y, tenue)
        for ((nombre, filas) in secciones) {
            espacio(30f)
            pagina!!.canvas.drawText(nombre, 50f, y, seccion)
            espacio(4f)
            for ((k, v) in filas) {
                if (v.isEmpty()) {
                    lineas("• $k", normal, 485f).forEach { espacio(16f); pagina!!.canvas.drawText(it, 60f, y, normal) }
                } else {
                    val izquierda = lineas(k, normal, 250f)
                    val derecha = lineas(v, normal, 225f)
                    val filasTexto = maxOf(izquierda.size, derecha.size)
                    espacio(16f)
                    for (i in 0 until filasTexto) {
                        if (i > 0) espacio(14f)
                        izquierda.getOrNull(i)?.let { pagina!!.canvas.drawText(it, 60f, y, normal) }
                        derecha.getOrNull(i)?.let { pagina!!.canvas.drawText(it, 320f, y, normal) }
                    }
                }
            }
        }
        pagina!!.canvas.drawText(contexto.getString(R.string.generado_con, contexto.getString(R.string.app_name)), 50f, 810f, tenue)
        documento.finishPage(pagina)

        val carpeta = File(contexto.cacheDir, "informes").apply { mkdirs() }
        val archivo = File(carpeta, titulo.replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_') + ".pdf")
        archivo.outputStream().use { documento.writeTo(it) }
        documento.close()

        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        contexto.startActivity(Intent.createChooser(intent, contexto.getString(R.string.exportar_pdf)))
    }
}
