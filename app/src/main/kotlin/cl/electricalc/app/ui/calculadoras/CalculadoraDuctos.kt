package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.datos.ric.Ric

/** Ocupación de tuberías y bandejas (RIC N°04, tablas N°4.17 a N°4.21 y numeral 7.10.19). */
object CalculadoraDuctos {
    private const val CABLE = "cable"
    private const val CANTIDAD = "cantidad"
    private const val DIAMETRO = "diametro"
    private const val AREA = "area_bandeja"

    /** Todas las filas de las tablas N°4.17 a N°4.20, en una sola lista: tipo de cable y sección. */
    private val filas by lazy { Ric.ductos.tablas.flatMap { t -> t.filas.map { t to it } } }

    private val cable = CampoOpciones(
        CABLE, R.string.campo_cable, listOf(R.string.ayuda_cable),
        opciones = Ric.ductos.tablas.flatMap { t ->
            val tipo = t.cables.take(2).joinToString("/")
            t.filas.map { f -> TextoOpcion.Literal("$tipo · ${f.seccion} ${if (t.unidad == "awg") "AWG" else "mm²"}") }
        },
        porDefecto = 1,
    )

    private val cantidad = CampoNumerico(
        CANTIDAD, R.string.campo_cantidad_conductores, listOf(R.string.ayuda_cantidad_conductores),
        unidades = { listOf(Unidad("")) }, porDefecto = { "" }, hint = { "3" }, inicial = { _, _ -> "3" },
    )

    private val diametro = CampoNumerico(
        DIAMETRO, R.string.campo_diametro_interior, listOf(R.string.ayuda_diametro_interior),
        unidades = { listOf(Unidad("mm")) }, porDefecto = { "mm" }, hint = { "16" },
    )

    private val area = CampoNumerico(
        AREA, R.string.campo_area_bandeja, listOf(R.string.ayuda_area_bandeja),
        unidades = { listOf(Unidad("mm²")) }, porDefecto = { "mm²" }, hint = { "5000" },
    )

    private val recomendado = Variante(listOf(CABLE, CANTIDAD), R.string.variante_ducto_recomendado)
    private val enTuberia = Variante(listOf(CABLE, DIAMETRO), R.string.variante_cantidad_tuberia)
    private val enBandeja = Variante(listOf(CABLE, AREA), R.string.variante_cantidad_bandeja)

    val ductos = Calculadora(
        id = "ocupacion_ductos",
        sistemas = listOf(SistemaElectrico.MONOFASICA),
        campos = listOf(cable, cantidad, diametro, area),
        variantes = { listOf(recomendado, enTuberia, enBandeja) },
        visible = { campo, _, variante, _ -> variante != null && campo.id in variante.campos },
        guia = Guia(listOf(R.string.intro_ductos)),
        calcular = { e ->
            val d = Ric.ductos
            val (tabla, fila) = filas[e.opcion(CABLE)]
            val datosCable = LineaResultado(
                R.string.res_cable, "Ø ${Formato.numero(fila.diametroMm, 1)} mm · ${Formato.numero(fila.areaMm2, 1)} mm² (tabla N°${tabla.tabla})",
                principal = false,
            )
            when (e.variante) {
                enTuberia -> listOf(
                    LineaResultado(R.string.res_cantidad_maxima, d.cantidadMaxima(e.valor(DIAMETRO), fila.areaMm2).toString()),
                    datosCable,
                )
                enBandeja -> listOf(
                    LineaResultado(R.string.res_cantidad_maxima, d.cantidadEnBandeja(e.valor(AREA), fila.areaMm2).toString()),
                    datosCable,
                )
                else -> {
                    val n = e.valor(CANTIDAD)
                    // Las tablas dan el ducto para 1 a 5 conductores.
                    if (n < 1 || n > 5 || n % 1.0 != 0.0) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
                    listOf(
                        LineaResultado(R.string.res_ducto_nominal, "${fila.ductosMm[n.toInt() - 1]} mm"),
                        datosCable,
                    )
                }
            }
        },
    )
}
