package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.datos.ric.CapacidadCorregida
import cl.electricalc.datos.ric.CapacidadRic
import cl.electricalc.datos.ric.Ric

/** Calculadoras que existen solo para el RIC: capacidad de corriente (N°04) y circuitos de vivienda (N°10). */
object CalculadorasRic {
    private val SIN_SISTEMA = listOf(SistemaElectrico.MONOFASICA)

    private const val AISLACION = "aislacion"
    private const val METODO = "metodo"
    private const val SECCION_TABLA = "seccion_tabla"
    private const val T_AMBIENTE = "t_ambiente"
    private const val CONDUCTORES = "conductores"
    private const val TIPO_CIRCUITO = "tipo_circuito"
    private const val CENTROS = "centros"
    private const val POTENCIA_CENTRO = "potencia_centro"

    /** Temperatura de servicio de cada opción de aislación (bloques de la tabla N°4.4). */
    private val temperaturas = listOf(70, 90)
    private val metodos = listOf("A1", "A2", "B1", "B2", "D1", "D2", "E", "F")

    /** Valores nominales comerciales usuales de termomagnéticas (IEC 60898-1); no son un valor del RIC. */
    val PROTECCIONES = listOf(6.0, 10.0, 16.0, 20.0, 25.0, 32.0, 40.0, 50.0, 63.0, 80.0, 100.0, 125.0)

    private val aislacion = CampoOpciones(
        AISLACION, R.string.campo_aislacion, listOf(R.string.ayuda_aislacion),
        opciones = listOf(R.string.aislacion_70, R.string.aislacion_90).map { TextoOpcion.Recurso(it) },
    )

    private val metodo = CampoOpciones(
        METODO, R.string.campo_metodo, listOf(R.string.ayuda_metodo),
        opciones = metodos.map { TextoOpcion.Literal(it) }, porDefecto = 2,
    )

    private val filas by lazy { Ric.capacidad.bloque(90)!!.filas }

    private val seccionTabla = CampoOpciones(
        SECCION_TABLA, R.string.campo_seccion, listOf(R.string.ayuda_seccion_tabla),
        opciones = Ric.capacidad.bloque(90)!!.filas.map { f ->
            TextoOpcion.Literal("${Formato.numero(f.mm2, 2)} mm²" + (f.awg?.let { " ($it AWG)" } ?: ""))
        },
        porDefecto = 2,
    )

    private val tAmbiente = CampoNumerico(
        T_AMBIENTE, R.string.campo_t_ambiente, listOf(R.string.ayuda_t_ambiente),
        unidades = { Unidades.temperatura }, porDefecto = { if (it.temperaturaEnFahrenheit) "°F" else "°C" },
        hint = { "30" }, inicial = { _, p -> if (p.temperaturaEnFahrenheit) "86" else "30" },
    )

    private val conductores = CampoNumerico(
        CONDUCTORES, R.string.campo_conductores_activos, listOf(R.string.ayuda_conductores_activos),
        unidades = { listOf(Unidad("")) }, porDefecto = { "" }, hint = { "3" }, inicial = { _, _ -> "3" },
    )

    private fun entero(e: Entrada, id: String): Int {
        val v = e.valor(id)
        if (v < 1 || v % 1.0 != 0.0) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return v.toInt()
    }

    private fun lineasCapacidad(c: CapacidadCorregida) = listOf(
        LineaResultado(R.string.res_ic, "${Formato.numero(c.ic, 1)} A"),
        LineaResultado(R.string.res_it, "${c.it} A", principal = false),
        LineaResultado(R.string.res_fn, Formato.numero(c.fn, 2), principal = false),
        LineaResultado(R.string.res_ft, Formato.numero(c.ft, 2), principal = false),
    )

    val capacidad = Calculadora(
        id = "capacidad_ric",
        sistemas = SIN_SISTEMA,
        campos = listOf(aislacion, metodo, seccionTabla, tAmbiente, conductores),
        visible = { _, _, _, _ -> true },
        guia = Guia(listOf(R.string.intro_capacidad_ric)),
        calcular = { e ->
            val c = CapacidadRic.corregida(
                temperaturas[e.opcion(AISLACION)], metodos[e.opcion(METODO)], filas[e.opcion(SECCION_TABLA)].mm2,
                e.valor(T_AMBIENTE), entero(e, CONDUCTORES),
            ) ?: throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
            lineasCapacidad(c)
        },
    )

    private val tipoCircuito = CampoOpciones(
        TIPO_CIRCUITO, R.string.campo_tipo_circuito, listOf(R.string.ayuda_tipo_circuito),
        opciones = listOf(
            R.string.circuito_iluminacion, R.string.circuito_enchufes, R.string.circuito_cocina, R.string.circuito_horno,
        ).map { TextoOpcion.Recurso(it) },
    )

    private val centros = CampoNumerico(
        CENTROS, R.string.campo_centros, listOf(R.string.ayuda_centros),
        unidades = { listOf(Unidad("")) }, porDefecto = { "" }, hint = { "8" },
    )

    private val potenciaCentro = CampoNumerico(
        POTENCIA_CENTRO, R.string.campo_potencia_centro, listOf(R.string.ayuda_potencia_centro),
        unidades = { Unidades.potenciaActiva }, porDefecto = { "W" }, hint = { "100 / 250" }, opcional = true,
    )

    val circuitosVivienda = Calculadora(
        id = "circuitos_vivienda",
        sistemas = SIN_SISTEMA,
        campos = listOf(tipoCircuito, Campos.tension, centros, potenciaCentro, aislacion, metodo, tAmbiente, conductores),
        visible = { _, _, _, _ -> true },
        guia = Guia(listOf(R.string.intro_circuitos_vivienda, R.string.intro_circuitos_vivienda_norma)),
        calcular = { e ->
            val reglas = Ric.circuitosAlumbrado
            val tipo = e.opcion(TIPO_CIRCUITO)
            val iluminacion = tipo == 0
            val tension = e.valor(Ids.TENSION)
            val n = entero(e, CENTROS)
            val pCentro = if (e.tiene(POTENCIA_CENTRO)) e.valor(POTENCIA_CENTRO)
            else if (iluminacion) reglas.potenciaCentroIluminacionW else reglas.potenciaEnchufeW
            val potencia = n * pCentro
            val requerida = reglas.corrienteRequerida(potencia, tension)
            // Cocina/lavadero y horno: circuito de al menos 16 A (numeral 5.2, letras d y e).
            val minimo = if (tipo >= 2) Ric.vivienda.circuitoCocinaMinA else 0.0
            val proteccion = PROTECCIONES.firstOrNull { it >= requerida && it >= minimo }
                ?: throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
            val minimaMm2 = Ric.seccionesMinimas.minima(if (iluminacion) "iluminacion" else "enchufes")!!.mm2
            val cable = CapacidadRic.seccionPara(
                proteccion, temperaturas[e.opcion(AISLACION)], metodos[e.opcion(METODO)], e.valor(T_AMBIENTE),
                entero(e, CONDUCTORES), minimaMm2,
            ) ?: throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
            val curva = reglas.curvas.getValue(if (iluminacion) "iluminacion" else "enchufes")
            listOf(
                LineaResultado(R.string.res_proteccion, "${Formato.numero(proteccion, 0)} A, curva $curva"),
                LineaResultado(R.string.res_seccion_conductor, "${Formato.numero(cable.mm2, 2)} mm²" + (cable.awg?.let { " ($it AWG)" } ?: "")),
                LineaResultado(R.string.res_diferencial, "≤ ${reglas.diferencialMaxMa} mA"),
                LineaResultado(R.string.res_potencia_circuito, Formato.conPrefijo(potencia, "W"), principal = false),
                LineaResultado(R.string.res_corriente_requerida, "${Formato.numero(requerida, 2)} A", principal = false),
                LineaResultado(R.string.res_centros_maximos, reglas.centrosMaximos(proteccion, tension, pCentro).toString(), principal = false),
                LineaResultado(R.string.res_ic_conductor, "${Formato.numero(cable.ic, 1)} A", principal = false),
                LineaResultado(R.string.res_circuitos_por_diferencial, "≤ ${reglas.circuitosPorDiferencialMax}", principal = false),
            )
        },
    )

    val todas = listOf(capacidad, circuitosVivienda)
}
