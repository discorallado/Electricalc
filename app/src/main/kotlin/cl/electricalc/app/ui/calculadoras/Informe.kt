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

/** Resumen de un cálculo (datos y resultados) para compartirlo como texto o PDF. */
class Informe(val titulo: String, val datos: List<Pair<String, String>>, val resultados: List<Pair<String, String>>) {

    fun comoTexto(contexto: Context): String = buildString {
        appendLine(titulo)
        appendLine()
        appendLine(contexto.getString(R.string.datos))
        datos.forEach { (k, v) -> appendLine("• $k: $v") }
        appendLine()
        appendLine(contexto.getString(R.string.resultado))
        resultados.forEach { (k, v) -> appendLine("• $k: $v") }
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

    /** Dibuja una página A4 con el resumen y la comparte. */
    fun compartirPdf(contexto: Context) {
        val documento = PdfDocument()
        val pagina = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val lienzo = pagina.canvas
        val titular = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val seccion = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val normal = Paint().apply { textSize = 12f; isAntiAlias = true }
        val tenue = Paint().apply { textSize = 10f; color = 0xFF666666.toInt(); isAntiAlias = true }
        var y = 60f
        lienzo.drawText(titulo, 50f, y, titular)
        y += 18f
        lienzo.drawText(DateFormat.getDateTimeInstance().format(Date()), 50f, y, tenue)
        fun bloque(nombre: String, filas: List<Pair<String, String>>) {
            y += 32f
            lienzo.drawText(nombre, 50f, y, seccion)
            y += 6f
            filas.forEach { (k, v) ->
                y += 20f
                lienzo.drawText(k, 60f, y, normal)
                lienzo.drawText(v, 330f, y, normal)
            }
        }
        bloque(contexto.getString(R.string.datos), datos)
        bloque(contexto.getString(R.string.resultado), resultados)
        lienzo.drawText(contexto.getString(R.string.generado_con, contexto.getString(R.string.app_name)), 50f, 800f, tenue)
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
