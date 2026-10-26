package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.energia.Compensacion
import cl.electricalc.calc.energia.Consumo
import cl.electricalc.calc.energia.Lumenes
import cl.electricalc.calc.energia.Respaldo

/** Campo numérico simple: una unidad fija o una lista, hint y valor inicial opcionales. */
fun campo(
    id: String,
    etiqueta: Int,
    ayuda: Int,
    unidades: List<Unidad> = listOf(Unidad("")),
    hint: String = "",
    inicial: String? = null,
    opcional: Boolean = false,
) = CampoNumerico(
    id, etiqueta, listOf(ayuda), unidades = { unidades }, porDefecto = { unidades.first().simbolo }, hint = { hint },
    inicial = inicial?.let { v -> { _, _ -> v } }, opcional = opcional,
)

/** Calculadora sin tipo de corriente y con todos sus campos visibles. */
fun calculadoraSimple(id: String, campos: List<Campo>, intro: List<Int>, calcular: (Entrada) -> List<LineaResultado>) = Calculadora(
    id = id, sistemas = listOf(SistemaElectrico.MONOFASICA), campos = campos,
    visible = { _, _, _, _ -> true }, guia = Guia(intro), calcular = calcular,
)

/** Energía, iluminación, factor de potencia y respaldo (fase D de docs/arquitectura-v2.md). */
object CalculadorasEnergia {
    private val porciento = Unidad("%") { it / 100 }

    val consumo = calculadoraSimple(
        "consumo_costo",
        listOf(
            campo("p", R.string.campo_potencia_equipo, R.string.ayuda_potencia_equipo, listOf(Unidades.W, Unidades.kW), "1500"),
            campo("h", R.string.campo_horas_dia, R.string.ayuda_horas_dia, listOf(Unidad("h")), "4"),
            campo("d", R.string.campo_dias, R.string.ayuda_dias, hint = "30", inicial = "30"),
            campo("t", R.string.campo_tarifa, R.string.ayuda_tarifa, listOf(Unidad("$/kWh")), "180"),
        ),
        listOf(R.string.intro_consumo),
    ) { e ->
        val kwh = Consumo.kwh(e.valor("p"), e.valor("h"), e.valor("d"))
        listOf(
            LineaResultado(R.string.res_energia, "${Formato.numero(kwh, 2)} kWh"),
            LineaResultado(R.string.res_costo, "$ ${Formato.numero(kwh * e.valor("t"), 0)}"),
            LineaResultado(R.string.res_costo_dia, "$ ${Formato.numero(kwh * e.valor("t") / e.valor("d"), 0)}", principal = false),
        )
    }

    val iluminacion = calculadoraSimple(
        "iluminacion",
        listOf(
            campo("e", R.string.campo_iluminancia, R.string.ayuda_iluminancia, listOf(Unidad("lx")), "500"),
            campo("l", R.string.campo_largo, R.string.ayuda_largo, listOf(Unidades.m), "6"),
            campo("a", R.string.campo_ancho, R.string.ayuda_largo, listOf(Unidades.m), "4"),
            campo("h", R.string.campo_altura_montaje, R.string.ayuda_altura_montaje, listOf(Unidades.m), "2,2"),
            campo("fl", R.string.campo_flujo_luminaria, R.string.ayuda_flujo_luminaria, listOf(Unidad("lm")), "3600"),
            campo("cu", R.string.campo_utilizacion, R.string.ayuda_utilizacion, hint = "0,6", inicial = Formato.numero(0.6)),
            campo("fm", R.string.campo_mantenimiento, R.string.ayuda_mantenimiento, hint = "0,8", inicial = Formato.numero(0.8)),
        ),
        listOf(R.string.intro_iluminacion),
    ) { e ->
        val s = e.valor("l") * e.valor("a")
        val flujo = Lumenes.flujoTotal(e.valor("e"), s, e.valor("cu"), e.valor("fm"))
        val n = Lumenes.luminarias(flujo, e.valor("fl"))
        listOf(
            LineaResultado(R.string.res_luminarias, n.toString()),
            LineaResultado(R.string.res_iluminancia_obtenida, "${Formato.numero(Lumenes.iluminanciaObtenida(n, e.valor("fl"), s, e.valor("cu"), e.valor("fm")), 0)} lx"),
            LineaResultado(R.string.res_flujo_total, "${Formato.numero(flujo, 0)} lm", principal = false),
            LineaResultado(R.string.res_indice_local, Formato.numero(Lumenes.indiceLocal(e.valor("l"), e.valor("a"), e.valor("h")), 2), principal = false),
        )
    }

    val factorPotencia = calculadoraSimple(
        "fp_recargo",
        listOf(
            campo("p", R.string.campo_potencia_activa, R.string.ayuda_potencia_activa, listOf(Unidades.kW, Unidades.W), "100"),
            campo("c1", R.string.campo_cos_actual, R.string.ayuda_cos_actual, hint = "0,8"),
            campo("c2", R.string.campo_cos_objetivo, R.string.ayuda_cos_objetivo, hint = "0,95"),
            campo("u", R.string.campo_tension_linea, R.string.ayuda_tension_linea, listOf(Unidades.V), "380", inicial = "380"),
        ),
        listOf(R.string.intro_fp),
    ) { e ->
        val q = Compensacion.reactiva(e.valor("p"), e.valor("c1"), e.valor("c2"))
        listOf(
            LineaResultado(R.string.res_reactiva_banco, Formato.conPrefijo(q, "var")),
            LineaResultado(R.string.res_capacidad_fase, Formato.conPrefijo(Compensacion.capacidadEstrella(q, e.valor("u")), "F"), principal = false),
        )
    }

    val respaldo = calculadoraSimple(
        "respaldo",
        listOf(
            campo("p", R.string.campo_carga_respaldada, R.string.ayuda_carga_respaldada, listOf(Unidades.W, Unidades.kW), "2000"),
            campo("cos", R.string.campo_factor_potencia, R.string.ayuda_factor_potencia, hint = "0,8", inicial = Formato.numero(0.8)),
            campo("r", R.string.campo_reserva, R.string.ayuda_reserva, listOf(porciento), "25", inicial = "25"),
            campo("t", R.string.campo_autonomia, R.string.ayuda_autonomia, listOf(Unidad("h")), "1"),
            campo("vb", R.string.campo_tension_banco, R.string.ayuda_tension_banco, listOf(Unidades.V), "48"),
            campo("eta", R.string.campo_rendimiento_inversor, R.string.ayuda_rendimiento_inversor, listOf(porciento), "90", inicial = "90"),
            campo("dod", R.string.campo_descarga, R.string.ayuda_descarga, listOf(porciento), "50", inicial = "50"),
        ),
        listOf(R.string.intro_respaldo),
    ) { e ->
        listOf(
            LineaResultado(R.string.res_potencia_equipo, Formato.conPrefijo(Respaldo.potenciaAparente(e.valor("p"), e.valor("cos"), e.valor("r")), "VA")),
            LineaResultado(
                R.string.res_capacidad_baterias,
                "${Formato.numero(Respaldo.capacidadBaterias(e.valor("p"), e.valor("t"), e.valor("vb"), e.valor("eta"), e.valor("dod")), 1)} Ah",
            ),
        )
    }

    val todas = listOf(consumo, iluminacion, factorPotencia, respaldo)
}

/** Ensayos de puesta en servicio y aislamiento mínimo (RIC N°19). */
object CalculadoraPuestaEnServicio {
    private val ps by lazy { cl.electricalc.datos.ric.Ric.puestaEnServicio }

    private val circuito = CampoOpciones(
        "circuito", R.string.campo_circuito_ensayo, listOf(R.string.ayuda_circuito_ensayo),
        opciones = listOf(R.string.circuito_mbts, R.string.circuito_hasta_500, R.string.circuito_sobre_500).map { TextoOpcion.Recurso(it) },
        porDefecto = 1,
    )

    val protocolo = Calculadora(
        id = "protocolo_puesta_servicio",
        sistemas = listOf(SistemaElectrico.MONOFASICA),
        campos = listOf(circuito, campo("r", R.string.campo_aislamiento_medido, R.string.ayuda_aislamiento_medido, listOf(Unidad("MΩ")), "500")),
        visible = { _, _, _, _ -> true },
        guia = Guia(listOf(R.string.intro_protocolo)),
        calcular = { e ->
            val fila = ps.aislamiento[e.opcion("circuito")]
            listOf(
                LineaResultado(R.string.res_tension_ensayo, "${fila.ensayoV} V CC · 1 min"),
                LineaResultado(R.string.res_aislamiento_minimo, "≥ ${Formato.numero(fila.minimoMohm, 2)} MΩ"),
            ) + ps.ensayos.mapIndexed { i, t -> LineaResultado(R.string.res_ensayo, "${i + 1}. $t", principal = false) } +
                ps.medicionesAislamiento.map { LineaResultado(R.string.res_medir, it, principal = false) }
        },
        verificar = { e ->
            listOf(cl.electricalc.datos.ric.VerificacionRic.aislamiento(e.valor("r"), ps.aislamiento[e.opcion("circuito")].circuito))
        },
    )
}

/** Dimensionamiento de tablero (RIC N°02): módulos con ampliación, bandeja interna y espacio de trabajo. */
object CalculadoraTablero {
    private val t by lazy { cl.electricalc.datos.ric.Ric.tableros }

    private val condicion = CampoOpciones(
        "condicion", R.string.campo_condicion_trabajo, listOf(R.string.ayuda_condicion_trabajo),
        opciones = listOf(R.string.condicion_1, R.string.condicion_2, R.string.condicion_3).map { TextoOpcion.Recurso(it) },
    )

    val tablero = Calculadora(
        id = "tablero",
        sistemas = listOf(SistemaElectrico.MONOFASICA),
        campos = listOf(
            campo("circuitos", R.string.campo_circuitos_tablero, R.string.ayuda_circuitos_tablero, hint = "10"),
            campo("modulos", R.string.campo_modulos_din, R.string.ayuda_modulos_din, listOf(Unidad("mód.")), "24"),
            condicion,
        ),
        visible = { _, _, _, _ -> true },
        guia = Guia(listOf(R.string.intro_tablero)),
        calcular = { e ->
            val circuitos = e.valor("circuitos").toInt()
            val modulos = e.valor("modulos").toInt()
            if (circuitos < 1 || modulos < 1) throw cl.electricalc.calc.comun.ErrorCalculo(cl.electricalc.calc.comun.MotivoError.VALOR_FUERA_DE_RANGO)
            val espacio = t.espaciosTrabajo[e.opcion("condicion")]
            listOf(
                LineaResultado(R.string.res_modulos_gabinete, "${t.modulosConReserva(modulos)} mód."),
                LineaResultado(R.string.res_espacio_trabajo, "${Formato.numero(espacio.m, 2)} m"),
                LineaResultado(
                    R.string.res_bandeja_interna,
                    if (t.requiereBandeja(circuitos)) "Sí, ocupación ≤ ${Formato.numero(t.bandejaOcupacionMax * 100, 0)} %" else "No exigida (< ${t.bandejaDesdeCircuitos} circuitos)",
                ),
                LineaResultado(R.string.res_referencia, "RIC N°02, numeral 6.1.16 y tabla N°2.1", principal = false),
            )
        },
    )
}
