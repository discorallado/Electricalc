package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.ohm.Magnitud

/** Identificadores de campo compartidos entre pantallas. */
object Ids {
    const val TENSION = "tension"
    const val CORRIENTE = "corriente"
    const val RESISTENCIA = "resistencia"
    const val IMPEDANCIA = "impedancia"
    const val POTENCIA = "potencia"
    const val POTENCIA_ACTIVA = "potencia_activa"
    const val POTENCIA_APARENTE = "potencia_aparente"
    const val POTENCIA_REACTIVA = "potencia_reactiva"
    const val FASE = "fase"
    const val CARGA = "carga"
    const val SECCION = "seccion"
    const val PARALELOS = "paralelos"
    const val LONGITUD = "longitud"
    const val MATERIAL = "material"
    const val TIPO_CABLE = "tipo_cable"
    const val TEMPERATURA = "temperatura"
    const val NUMERO_CARGAS = "numero_cargas"
    const val CAIDA_MAXIMA = "caida_maxima"
}

/** Campos comunes. Las pantallas los combinan; cada una decide cuáles muestra. */
object Campos {
    private fun tensionHint(s: SistemaElectrico) = when (s) {
        SistemaElectrico.CONTINUA -> "24"
        SistemaElectrico.MONOFASICA -> "220"
        else -> "380"
    }

    val tension = CampoNumerico(
        Ids.TENSION, R.string.campo_tension, listOf(R.string.ayuda_tension, R.string.ayuda_tension_sistema),
        unidades = { Unidades.tension }, porDefecto = { "V" }, magnitud = Magnitud.TENSION,
        hint = ::tensionHint, inicial = { s, p -> p.tensionPorDefecto(s) },
    )

    val corriente = CampoNumerico(
        Ids.CORRIENTE, R.string.campo_corriente, listOf(R.string.ayuda_corriente),
        unidades = { Unidades.corriente }, porDefecto = { "A" }, magnitud = Magnitud.CORRIENTE, hint = { "10" },
    )

    val resistencia = CampoNumerico(
        Ids.RESISTENCIA, R.string.campo_resistencia, listOf(R.string.ayuda_resistencia),
        unidades = { Unidades.resistencia }, porDefecto = { "Ω" }, magnitud = Magnitud.RESISTENCIA, hint = { "22" },
    )

    val impedancia = CampoNumerico(
        Ids.IMPEDANCIA, R.string.campo_impedancia, listOf(R.string.ayuda_impedancia),
        unidades = { Unidades.resistencia }, porDefecto = { "Ω" }, magnitud = Magnitud.IMPEDANCIA, hint = { "25" },
    )

    /** Potencia cuyo tipo (P, S o Q) lo fija la unidad elegida. */
    val potencia = CampoNumerico(
        Ids.POTENCIA, R.string.campo_potencia, listOf(R.string.ayuda_potencia, R.string.ayuda_potencia_unidades),
        unidades = Unidades::potencia, porDefecto = { "W" }, hint = { "2200" },
    )

    val potenciaActiva = CampoNumerico(
        Ids.POTENCIA_ACTIVA, R.string.campo_potencia_activa, listOf(R.string.ayuda_potencia_activa),
        unidades = { Unidades.potenciaActiva }, porDefecto = { "W" }, magnitud = Magnitud.POTENCIA_ACTIVA,
        hint = { "2000" },
    )

    val potenciaAparente = CampoNumerico(
        Ids.POTENCIA_APARENTE, R.string.campo_potencia_aparente, listOf(R.string.ayuda_potencia_aparente),
        unidades = { Unidades.potenciaAparente }, porDefecto = { "VA" }, magnitud = Magnitud.POTENCIA_APARENTE,
        hint = { "2500" },
    )

    val potenciaReactiva = CampoNumerico(
        Ids.POTENCIA_REACTIVA, R.string.campo_potencia_reactiva, listOf(R.string.ayuda_potencia_reactiva),
        unidades = { Unidades.potenciaReactiva }, porDefecto = { "var" }, magnitud = Magnitud.POTENCIA_REACTIVA,
        hint = { "1500" },
    )

    private val ayudaFase = listOf(R.string.ayuda_factor_potencia, R.string.ayuda_factor_potencia_valores)

    /** cosφ con selector para ingresarlo también como senφ, tanφ o ángulo. */
    val fase = CampoNumerico(
        Ids.FASE, R.string.campo_factor_potencia, ayudaFase + R.string.ayuda_factor_potencia_formas,
        unidades = { Unidades.fase }, porDefecto = { "cos φ" }, hint = { Formato.numero(0.9) },
        inicial = { s, p -> if (s.esAlterna) Formato.leer(p.cosPhiPorDefecto)?.let(Formato::numero) ?: p.cosPhiPorDefecto else "" },
    )

    /** cosφ sin otras formas de ingreso (caída de tensión). */
    val cosPhi = CampoNumerico(
        Ids.FASE, R.string.campo_factor_potencia, ayudaFase,
        unidades = { listOf(Unidades.cosPhi) }, porDefecto = { "cos φ" }, hint = { Formato.numero(0.9) },
        inicial = { s, p -> if (s.esAlterna) Formato.leer(p.cosPhiPorDefecto)?.let(Formato::numero) ?: p.cosPhiPorDefecto else "" },
    )
}
