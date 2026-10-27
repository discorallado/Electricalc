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
fun calculadoraSimple(
    id: String,
    campos: List<Campo>,
    intro: List<Int>,
    verificar: (Entrada) -> List<cl.electricalc.datos.ric.Verificacion> = { emptyList() },
    calcular: (Entrada) -> List<LineaResultado>,
) = Calculadora(
    id = id, sistemas = listOf(SistemaElectrico.MONOFASICA), campos = campos,
    visible = { _, _, _, _ -> true }, guia = Guia(intro), calcular = calcular, verificar = verificar,
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
                LineaResultado.texto(R.string.res_modulos_gabinete, R.string.val_modulos, t.modulosConReserva(modulos)),
                LineaResultado(R.string.res_espacio_trabajo, "${Formato.numero(espacio.m, 2)} m"),
                if (t.requiereBandeja(circuitos)) LineaResultado.texto(R.string.res_bandeja_interna, R.string.val_bandeja_si, Formato.numero(t.bandejaOcupacionMax * 100, 0))
                else LineaResultado.texto(R.string.res_bandeja_interna, R.string.val_bandeja_no, t.bandejaDesdeCircuitos),
                LineaResultado(R.string.res_referencia, "RIC N°02, numeral 6.1.16 y tabla N°2.1", principal = false),
            )
        },
    )
}

/** Protecciones de un sistema de autogeneración conectado a la red (RIC N°09, anexo 9.1). */
object CalculadoraAutogeneracion {
    private val a by lazy { cl.electricalc.datos.ric.Ric.autogeneracion }

    private fun siNo(id: String, etiqueta: Int, ayuda: Int, porDefecto: Int) =
        CampoOpciones(id, etiqueta, listOf(ayuda), listOf(R.string.si, R.string.no).map { TextoOpcion.Recurso(it) }, porDefecto)

    private fun exigida(b: Boolean) = if (b) R.string.val_exigida else R.string.val_no_exigida

    val fv = Calculadora(
        id = "fv_netbilling",
        sistemas = listOf(SistemaElectrico.MONOFASICA),
        campos = listOf(
            campo("p", R.string.campo_potencia_generacion, R.string.ayuda_potencia_generacion, listOf(Unidades.kW), "3"),
            campo("emp", R.string.campo_empalme_fv, R.string.ayuda_empalme_fv, listOf(Unidades.kW), "10"),
            siNo("inv", R.string.campo_con_inversor, R.string.ayuda_con_inversor, 0),
            siNo("cero", R.string.campo_inyeccion_cero, R.string.ayuda_inyeccion_cero, 1),
        ),
        visible = { _, _, _, _ -> true },
        guia = Guia(listOf(R.string.intro_autogeneracion)),
        calcular = { e ->
            val kw = e.valor("p") / 1000
            val r = a.para(kw, e.valor("emp") / 1000, e.opcion("inv") == 0, e.opcion("cero") == 0)
            listOf(
                LineaResultado.texto(R.string.res_diferencial_fv, R.string.val_diferencial_ab, r.diferencialMa),
                LineaResultado.texto(R.string.res_proteccion_ri, if (r.riCentralizada) R.string.val_ri_centralizada else R.string.val_ri_complementaria),
                r.tiempoInversaS?.let { LineaResultado.texto(R.string.res_potencia_inversa, R.string.val_inversa, Formato.numero(it, 0), Formato.conPrefijo(r.pickupKw * 1000, "W")) }
                    ?: LineaResultado.texto(R.string.res_potencia_inversa, R.string.val_inversa_exenta),
                LineaResultado.texto(R.string.res_registrador, exigida(r.registrador), principal = false),
                LineaResultado.texto(R.string.res_responsable_tecnico, exigida(r.responsableTecnico), principal = false),
                LineaResultado(R.string.res_referencia, "RIC N°09, numerales 6.4.2.4, 6.6.7, 6.6.8 y anexo 9.1", principal = false),
            )
        },
    )
}

/** Volumen de un punto en un baño o ducha y lo que admite (RIC N°11, numeral 6.4). */
object CalculadoraVolumenesBano {
    private val v by lazy { cl.electricalc.datos.ric.Ric.volumenesBano }

    private val tipo = CampoOpciones(
        "tipo", R.string.campo_tipo_ducha, listOf(R.string.ayuda_tipo_ducha),
        listOf(R.string.ducha_receptaculo, R.string.ducha_difusor_fijo, R.string.ducha_difusor_movil).map { TextoOpcion.Recurso(it) },
    )

    val volumenes = calculadoraSimple(
        "volumenes_bano",
        listOf(
            tipo,
            campo("d", R.string.campo_distancia_ducha, R.string.ayuda_distancia_ducha, listOf(Unidades.m), "0,5"),
            campo("h", R.string.campo_altura_punto, R.string.ayuda_altura_punto, listOf(Unidades.m), "1,2"),
            campo("techo", R.string.campo_altura_techo, R.string.ayuda_altura_techo, listOf(Unidades.m), "2,4", opcional = true),
        ),
        listOf(R.string.intro_volumenes_bano),
    ) { e ->
        val t = cl.electricalc.datos.ric.TipoDucha.entries[e.opcion("tipo")]
        val techo = if (e.tiene("techo")) e.valor("techo") else null
        val n = v.volumen(t, e.valor("d"), e.valor("h"), techo)
            ?: return@calculadoraSimple listOf(
                LineaResultado.texto(R.string.res_volumen, R.string.val_fuera_volumenes),
                LineaResultado(R.string.res_referencia, "RIC N°11, numeral 6.4.1", principal = false),
            )
        val r = v.requisitos(n)
        listOf(
            LineaResultado.texto(R.string.res_volumen, R.string.val_volumen, n),
            LineaResultado(R.string.res_grado_ip, r.ip),
            LineaResultado(R.string.res_mecanismos, r.mecanismos, principal = false),
            LineaResultado(R.string.res_aparatos, r.aparatos, principal = false),
            LineaResultado(R.string.res_cableado, r.cableado, principal = false),
            LineaResultado(R.string.res_referencia, "RIC N°11, numerales 6.4.1 y 6.4.3", principal = false),
        )
    }
}

/** Impedancia de bucle de falla medida frente al máximo del RIC N°19 (tabla 19.2.1) y Ipcc presunta. */
object CalculadoraImpedanciaBucle {
    private val b by lazy { cl.electricalc.datos.ric.Ric.impedanciaBucle }
    private val corrientes by lazy { b.filas.map { it.inA } }
    private const val CURVAS = "BCD"

    val bucle = calculadoraSimple(
        "impedancia_bucle",
        listOf(
            CampoOpciones("esquema", R.string.campo_esquema_bucle, listOf(R.string.ayuda_esquema_bucle), listOf("TN", "TT").map { TextoOpcion.Literal(it) }),
            CampoOpciones("curva", R.string.campo_curva, listOf(R.string.ayuda_curva), CURVAS.map { TextoOpcion.Literal("$it") }, 1),
            CampoOpciones("in", R.string.campo_in_bucle, listOf(R.string.ayuda_in_bucle), listOf(2, 4, 6, 10, 16, 20, 25, 32, 35, 40, 50, 63).map { TextoOpcion.Literal("$it A") }, 4),
            campo("zs", R.string.campo_zs_medida, R.string.ayuda_zs_medida, listOf(Unidades.ohm), "1,2"),
        ),
        listOf(R.string.intro_bucle),
        verificar = { e ->
            listOf(cl.electricalc.datos.ric.VerificacionRic.bucle(e.valor("zs"), e.opcion("esquema") == 1, corrientes[e.opcion("in")], CURVAS[e.opcion("curva")]))
        },
    ) { e ->
            val curva = CURVAS[e.opcion("curva")]
            val inA = corrientes[e.opcion("in")]
            val tt = e.opcion("esquema") == 1
            val maxima = if (tt) b.ttMaxOhm else b.maxima(inA, curva)
            listOf(
                LineaResultado(R.string.res_zs_maxima, "${if (tt) "<" else "≤"} ${Formato.numero(maxima, 2)} Ω"),
                LineaResultado(R.string.res_ipcc, Formato.conPrefijo(b.ipcc(e.valor("zs")), "A")),
                LineaResultado(R.string.res_corriente_disparo, "${inA * b.multiplicador.getValue(curva.toString())} A", principal = false),
                LineaResultado(R.string.res_referencia, "RIC N°19, anexo 19.2, tabla 19.2.1", principal = false),
            )
    }
}

/** Transformador de subestación: potencia, corrientes, cortocircuito en BT y exigencias del RIC N°13. */
object CalculadoraTransformador {
    private val t by lazy { cl.electricalc.datos.ric.Ric.transformadores }
    private val porciento = Unidad("%") { it / 100 }

    val transformador = calculadoraSimple(
        "transformador_se",
        listOf(
            campo("p", R.string.campo_demanda_maxima, R.string.ayuda_demanda_trafo, listOf(Unidades.kW, Unidades.W), "120"),
            campo("cos", R.string.campo_factor_potencia, R.string.ayuda_factor_potencia, hint = "0,9", inicial = Formato.numero(0.9)),
            campo("r", R.string.campo_reserva, R.string.ayuda_reserva, listOf(porciento), "20", inicial = "20"),
            campo("s", R.string.campo_potencia_trafo, R.string.ayuda_potencia_trafo, listOf(Unidades.kVA), "150", opcional = true),
            campo("u1", R.string.campo_tension_primario, R.string.ayuda_tension_primario, listOf(Unidades.kV, Unidades.V), "12", inicial = "12"),
            campo("u2", R.string.campo_tension_secundario, R.string.ayuda_tension_secundario, listOf(Unidades.V), "380", inicial = "380"),
            campo("z", R.string.campo_impedancia_trafo, R.string.ayuda_impedancia_trafo, listOf(porciento), "4", inicial = "4"),
            CampoOpciones("tipo", R.string.campo_tipo_trafo, listOf(R.string.ayuda_tipo_trafo), listOf(R.string.trafo_seco, R.string.trafo_aceite).map { TextoOpcion.Recurso(it) }),
        ),
        listOf(R.string.intro_transformador),
    ) { e ->
        val requerida = e.valor("p") / e.valor("cos") * (1 + e.valor("r"))
        val s = if (e.tiene("s")) e.valor("s") else requerida
        val kva = s / 1000
        val raiz3 = kotlin.math.sqrt(3.0)
        val i1 = s / (raiz3 * e.valor("u1"))
        val i2 = s / (raiz3 * e.valor("u2"))
        val z = e.valor("z")
        val p = t.tabla133(z)
        val bt = e.valor("u2") <= 600
        listOf(
            LineaResultado(R.string.res_potencia_requerida, Formato.conPrefijo(requerida, "VA")),
            LineaResultado(R.string.res_in_primario, Formato.conPrefijo(i1, "A")),
            LineaResultado(R.string.res_in_secundario, Formato.conPrefijo(i2, "A")),
            LineaResultado(R.string.res_icc_secundario, Formato.conPrefijo(i2 / z, "A")),
            LineaResultado.texto(
                R.string.res_proteccion_primario, R.string.val_proteccion_primario,
                Formato.conPrefijo(i1 * t.fusiblePrimarioMaxIn, "A"), Formato.conPrefijo(i1 * t.automaticoPrimarioMaxIn, "A"),
            ),
            if (bt) {
                LineaResultado.texto(
                    R.string.res_sin_proteccion_individual, R.string.val_sin_individual_bt,
                    Formato.numero(p.primarioAutomatico, 0), Formato.numero(p.primarioFusible, 0),
                    Formato.numero(p.secundarioBt, 1), Formato.conPrefijo(i2 * p.secundarioBt, "A"), principal = false,
                )
            } else {
                LineaResultado.texto(
                    R.string.res_sin_proteccion_individual, R.string.val_sin_individual_mt,
                    Formato.numero(p.primarioAutomatico, 0), Formato.numero(p.primarioFusible, 0),
                    Formato.numero(p.secundarioMtAutomatico, 1), Formato.numero(p.secundarioMtFusible, 2), principal = false,
                )
            },
            LineaResultado.texto(
                R.string.res_ventilacion, R.string.val_ventilacion,
                Formato.numero(t.ventilacionM2(kva), 2), Formato.numero(t.renovacionesHora, 0), principal = false,
            ),
            LineaResultado.texto(
                R.string.res_vigilancia_termica,
                when {
                    !t.vigilanciaTermica(kva, e.opcion("tipo") == 1) -> R.string.val_no_exigida
                    e.opcion("tipo") == 1 -> R.string.val_buchholz
                    else -> R.string.val_pt100
                },
                principal = false,
            ),
            LineaResultado(R.string.res_referencia, "RIC N°13, numerales 8.10, 9.7, 9.8, 13.2 a 13.5 y tabla N°13.3", principal = false),
        )
    }
}

/** Asistente de clasificación de zonas con riesgo de explosión (RIC N°12). */
object CalculadoraAreasPeligrosas {
    private val a by lazy { cl.electricalc.datos.ric.Ric.areasPeligrosas }

    val areas = calculadoraSimple(
        "areas_peligrosas",
        listOf(
            CampoOpciones("sustancia", R.string.campo_sustancia, listOf(R.string.ayuda_sustancia), listOf(R.string.sustancia_gas, R.string.sustancia_polvo).map { TextoOpcion.Recurso(it) }),
            CampoOpciones(
                "presencia", R.string.campo_presencia, listOf(R.string.ayuda_presencia),
                listOf(R.string.presencia_permanente, R.string.presencia_ocasional, R.string.presencia_rara).map { TextoOpcion.Recurso(it) }, 1,
            ),
            campo("iz", R.string.campo_iz_convencional, R.string.ayuda_iz_convencional, listOf(Unidades.A), "32", opcional = true),
        ),
        listOf(R.string.intro_areas),
    ) { e ->
        val polvo = e.opcion("sustancia") == 1
        val z = a.zonas[e.opcion("presencia")]
        listOfNotNull(
            LineaResultado.texto(R.string.res_zona, R.string.val_zona, if (polvo) z.polvo else z.gas, if (polvo) "III" else "II"),
            LineaResultado.texto(R.string.res_nivel_proteccion, listOf(R.string.val_nivel_muy_alto, R.string.val_nivel_alto, R.string.val_nivel_normal)[z.nivel]),
            if (e.tiene("iz")) LineaResultado(R.string.res_iz_reducida, Formato.conPrefijo(a.corrienteAdmisible(e.valor("iz")), "A")) else null,
        ) + a.modos(z.nivel, polvo).map { LineaResultado(R.string.res_modo_admitido, it, principal = false) } +
            LineaResultado(R.string.res_referencia, "RIC N°12, numerales 5, 7.5 y 11.1.5", principal = false)
    }
}

/** Plan de mantenimiento: programa de seguridad eléctrica y trabajo sin tensión (RIC N°17). */
object CalculadoraMantenimiento {
    private val m by lazy { cl.electricalc.datos.ric.Ric.mantenimiento }

    val plan = calculadoraSimple(
        "plan_mantenimiento",
        listOf(
            campo("p", R.string.campo_potencia_instalada_mant, R.string.ayuda_potencia_instalada_mant, listOf(Unidades.kW), "150"),
            CampoOpciones("mt", R.string.campo_empalme_mt, listOf(R.string.ayuda_empalme_mt), listOf(R.string.si, R.string.no).map { TextoOpcion.Recurso(it) }, 1),
            CampoOpciones("uso", R.string.campo_uso_instalacion, listOf(R.string.ayuda_uso_instalacion), listOf(R.string.uso_no_residencial, R.string.uso_vivienda).map { TextoOpcion.Recurso(it) }),
        ),
        listOf(R.string.intro_mantenimiento),
    ) { e ->
        val exige = m.exigePrograma(e.valor("p") / 1000, e.opcion("mt") == 0, e.opcion("uso") == 0)
        listOf(LineaResultado.texto(R.string.res_programa_seguridad, if (exige) R.string.val_obligatorio else R.string.val_recomendado)) +
            m.componentes.mapIndexed { i, c -> LineaResultado(R.string.res_componente, "${i + 1}. $c", principal = false) } +
            m.reglasOro.mapIndexed { i, r -> LineaResultado(R.string.res_regla_oro, "${i + 1}. $r", principal = false) } +
            LineaResultado(R.string.res_referencia, "RIC N°17, numerales 5.3, 6.1 y 6.3.2", principal = false)
    }
}

/** Misma pantalla con otro id del catálogo (herramientas del APK que ya cubre una calculadora). */
fun alias(id: String, base: Calculadora) = Calculadora(
    id = id, sistemas = base.sistemas, campos = base.campos, variantes = base.variantes, visible = base.visible,
    guia = base.guia, calcular = base.calcular, verificar = base.verificar,
)

/** Autonomía de un banco de baterías y herramientas del APK cubiertas por otras pantallas. */
object CalculadorasRespaldo {
    private val porciento = Unidad("%") { it / 100 }

    val duracion = calculadoraSimple(
        "DurataBatteria",
        listOf(
            campo("c", R.string.campo_capacidad_ah, R.string.ayuda_capacidad_banco, listOf(Unidad("Ah")), "200"),
            campo("vb", R.string.campo_tension_banco, R.string.ayuda_tension_banco, listOf(Unidades.V), "24"),
            campo("p", R.string.campo_carga_respaldada, R.string.ayuda_carga_respaldada, listOf(Unidades.W, Unidades.kW), "500"),
            campo("eta", R.string.campo_rendimiento_inversor, R.string.ayuda_rendimiento_inversor, listOf(porciento), "90", inicial = "90"),
            campo("dod", R.string.campo_descarga, R.string.ayuda_descarga, listOf(porciento), "50", inicial = "50"),
        ),
        listOf(R.string.intro_duracion_bateria),
    ) { e ->
        val h = cl.electricalc.calc.energia.Respaldo.autonomia(e.valor("c"), e.valor("vb"), e.valor("p"), e.valor("eta"), e.valor("dod"))
        listOf(
            LineaResultado(R.string.res_autonomia, "${Formato.numero(h, 2)} h"),
            LineaResultado(R.string.res_autonomia_min, "${Formato.numero(h * 60, 0)} min", principal = false),
            LineaResultado(R.string.res_energia_util, "${Formato.numero(e.valor("c") * e.valor("vb") * e.valor("dod") / 1000, 2)} kWh", principal = false),
        )
    }

    val todas = listOf(
        duracion,
        alias("Rifasamento", CalculadorasEnergia.factorPotencia),
        alias("ImpiantoDiTerra", CalculadorasProteccion.tt),
    )
}
