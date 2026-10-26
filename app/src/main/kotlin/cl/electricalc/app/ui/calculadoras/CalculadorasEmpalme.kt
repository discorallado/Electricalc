package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.datos.ric.Ric
import kotlin.math.sqrt

/** Empalme (RIC N°01 y N°10) y carga de vehículos eléctricos (RIC N°15). */
object CalculadorasEmpalme {
    private val SISTEMAS = listOf(SistemaElectrico.MONOFASICA, SistemaElectrico.TRIFASICA)

    private const val INSTALADA = "instalada"
    private const val DEMANDA = "demanda"
    private const val VIVIENDA = "vivienda"
    private const val POTENCIA_SAVE = "potencia_save"
    private const val CANTIDAD = "cantidad"
    private const val MODO = "modo"
    private const val TIPO_IRVE = "tipo_irve"
    private const val SGC = "sgc"
    private const val EMPALME_KW = "empalme_kw"

    private fun kw(id: String, etiqueta: Int, ayuda: Int, hint: String, opcional: Boolean = false) = CampoNumerico(
        id, etiqueta, listOf(ayuda), unidades = { listOf(Unidades.kW, Unidades.W) }, porDefecto = { "kW" },
        hint = { hint }, opcional = opcional,
    )

    private val instalada = kw(INSTALADA, R.string.campo_potencia_instalada, R.string.ayuda_potencia_instalada, "8")
    private val demanda = kw(DEMANDA, R.string.campo_demanda_maxima, R.string.ayuda_demanda_maxima, "5", opcional = true)
    private val vivienda = CampoOpciones(
        VIVIENDA, R.string.campo_es_vivienda, listOf(R.string.ayuda_es_vivienda),
        opciones = listOf(R.string.no, R.string.si).map { TextoOpcion.Recurso(it) }, porDefecto = 1,
    )

    val empalme = Calculadora(
        id = "seleccion_empalme",
        sistemas = SISTEMAS,
        campos = listOf(instalada, demanda, vivienda),
        visible = { _, _, _, _ -> true },
        guia = Guia(listOf(R.string.intro_empalme, R.string.intro_empalme_norma)),
        calcular = { e ->
            val tri = e.sistema == SistemaElectrico.TRIFASICA
            val instaladaKw = e.valor(INSTALADA) / 1000
            val demandaKw = if (e.tiene(DEMANDA)) e.valor(DEMANDA) / 1000 else instaladaKw
            if (demandaKw > instaladaKw + 1e-9) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
            // Vivienda: protección del empalme de al menos 25 A (RIC N°10, numeral 5.2).
            val minimoA = if (e.opcion(VIVIENDA) == 1) Ric.vivienda.empalmeProteccionMinA else 0.0
            val elegido = Ric.empalmes.para(demandaKw, tri, minimoA) ?: throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
            listOf(
                LineaResultado(R.string.res_empalme, "${Formato.numero(elegido.a, 0)} A · ${Formato.numero(elegido.kw, 1)} kW"),
                LineaResultado(R.string.res_potencia_maxima_empalme, "${Formato.numero(elegido.kva, 2)} kVA", principal = false),
                LineaResultado(R.string.res_demanda_usada, "${Formato.numero(demandaKw, 2)} kW", principal = false),
            )
        },
    )

    private val potenciaSave = kw(POTENCIA_SAVE, R.string.campo_potencia_save, R.string.ayuda_potencia_save, "7,4")
    private val cantidad = CampoNumerico(
        CANTIDAD, R.string.campo_cantidad_cargadores, listOf(R.string.ayuda_cantidad_cargadores),
        unidades = { listOf(Unidad("")) }, porDefecto = { "" }, hint = { "1" }, inicial = { _, _ -> "1" },
    )
    private val modo = CampoOpciones(
        MODO, R.string.campo_modo_carga, listOf(R.string.ayuda_modo_carga),
        opciones = (1..4).map { TextoOpcion.Literal("Modo $it") }, porDefecto = 2,
    )
    private val tipoIrve = CampoOpciones(
        TIPO_IRVE, R.string.campo_tipo_irve, listOf(R.string.ayuda_tipo_irve),
        opciones = listOf(R.string.irve_residencial, R.string.irve_publico, R.string.irve_transporte).map { TextoOpcion.Recurso(it) },
    )
    private val sgc = CampoOpciones(
        SGC, R.string.campo_sgc, listOf(R.string.ayuda_sgc),
        opciones = listOf(R.string.no, R.string.si).map { TextoOpcion.Recurso(it) },
    )
    private val empalmeKw = kw(EMPALME_KW, R.string.campo_capacidad_empalme, R.string.ayuda_capacidad_empalme, "10", opcional = true)

    val cargaVe = Calculadora(
        id = "carga_ve",
        sistemas = SISTEMAS,
        campos = listOf(Campos.tension, potenciaSave, cantidad, modo, tipoIrve, sgc, empalmeKw),
        visible = { _, _, _, _ -> true },
        guia = Guia(listOf(R.string.intro_carga_ve)),
        calcular = { e ->
            val v = Ric.cargaVe
            val p = e.valor(POTENCIA_SAVE)
            val n = e.valor(CANTIDAD).also { if (it < 1 || it % 1.0 != 0.0) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO) }
            val k = if (e.sistema == SistemaElectrico.TRIFASICA) sqrt(3.0) else 1.0
            val inA = p / (k * e.valor(Ids.TENSION))
            val ajuste = v.factorProteccion(inA) * inA
            val proteccion = CalculadorasRic.PROTECCIONES.firstOrNull { it >= ajuste - 1e-9 }
            val totalKw = n * p / 1000
            val tipo = v.tipos[e.opcion(TIPO_IRVE)].tipo
            // Sin sistema de gestión de carga el factor de demanda es 1 (numeral 9.4.1).
            val (minKw, maxKw) = if (e.opcion(SGC) == 1) v.demandaConSgc(tipo, totalKw) else totalKw to totalKw
            val dif = Ric.diferencialesVe.modo(e.opcion(MODO) + 1)
            listOfNotNull(
                LineaResultado(R.string.res_corriente_save, "${Formato.numero(inA, 1)} A"),
                LineaResultado(
                    R.string.res_proteccion,
                    (proteccion?.let { "${Formato.numero(it, 0)} A" } ?: "≥ ${Formato.numero(ajuste, 1)} A") +
                        " (${Formato.numero(v.factorProteccion(inA), 2)} × In)",
                ),
                LineaResultado(R.string.res_diferencial, "Tipo ${dif.tipo}, ≤ ${dif.sensibilidadMa} mA"),
                LineaResultado(
                    R.string.res_demanda_ve,
                    if (minKw == maxKw) "${Formato.numero(maxKw, 2)} kW" else "${Formato.numero(minKw, 2)} – ${Formato.numero(maxKw, 2)} kW",
                ),
                if (e.tiene(EMPALME_KW)) {
                    LineaResultado(R.string.res_margen_empalme, "${Formato.numero(e.valor(EMPALME_KW) / 1000 - maxKw, 2)} kW", principal = false)
                } else {
                    null
                },
                dif.alternativa?.let { LineaResultado(R.string.res_alternativa, it, principal = false) },
            )
        },
    )

    val todas = listOf(empalme, cargaVe)
}
