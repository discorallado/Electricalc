package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.caida.CaidaTension
import cl.electricalc.calc.comun.ErrorCalculo
import cl.electricalc.calc.comun.MotivoError
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.conductores.Cable
import cl.electricalc.calc.conductores.Material
import cl.electricalc.calc.conductores.Seccion
import cl.electricalc.calc.conductores.TablasConductores
import cl.electricalc.calc.conductores.TipoCable
import cl.electricalc.calc.ohm.LeyOhm
import cl.electricalc.calc.ohm.Magnitud
import cl.electricalc.datos.ric.Ric
import cl.electricalc.datos.ric.Verificacion
import cl.electricalc.datos.ric.VerificacionRic
import kotlin.math.PI
import kotlin.math.pow

/**
 * Las tres pantallas sobre [CaidaTension]: caída con la carga al final, caída con cargas iguales
 * repartidas a lo largo de la línea y longitud máxima para una caída dada. Comparten los campos
 * del conductor.
 */
object CalculadoraCaida {
    /** Unidades del campo de sección; el índice de la opción apunta a la tabla de cada una. */
    const val MM2 = "mm²"
    const val AWG = "AWG"

    /** Caída máxima expresada en porcentaje de la tensión de alimentación. */
    const val PORCENTAJE = "%"

    private val carga = CampoNumerico(
        Ids.CARGA, R.string.campo_carga, listOf(R.string.ayuda_carga, R.string.ayuda_carga_unidades),
        unidades = Unidades::carga, porDefecto = { "W" }, hint = { "3000" },
    )

    private val cargaUnitaria = CampoNumerico(
        Ids.CARGA, R.string.campo_carga_unitaria, listOf(R.string.ayuda_carga_unitaria, R.string.ayuda_carga_unidades),
        unidades = Unidades::carga, porDefecto = { "W" }, hint = { "100" },
    )

    private val numeroCargas = CampoNumerico(
        Ids.NUMERO_CARGAS, R.string.campo_numero_cargas, listOf(R.string.ayuda_numero_cargas),
        unidades = { listOf(Unidad("")) }, porDefecto = { "" }, hint = { "10" },
    )

    private val caidaMaxima = CampoNumerico(
        Ids.CAIDA_MAXIMA, R.string.campo_caida_maxima, listOf(R.string.ayuda_caida_maxima, R.string.ayuda_caida_maxima_norma),
        unidades = { listOf(Unidad(PORCENTAJE), Unidades.V) }, porDefecto = { PORCENTAJE },
        // El límite por tramo del RIC N°03 (numeral 5.1.3), con el separador decimal del idioma.
        inicial = { _, _ -> Formato.numero(Ric.caidas.porTramo) },
    )

    /** Uso de la línea, para verificar la sección mínima del RIC N°04 (numeral 5.4). La opción 0 es "sin indicar". */
    private val usos = listOf(null, "iluminacion", "enchufes", "mixto", "subalimentador", "alimentador")

    private val uso = CampoOpciones(
        Ids.USO, R.string.campo_uso_linea, listOf(R.string.ayuda_uso_linea),
        opciones = listOf(
            R.string.uso_sin_indicar, R.string.uso_iluminacion, R.string.uso_enchufes, R.string.uso_mixto,
            R.string.uso_subalimentador, R.string.uso_alimentador,
        ).map { TextoOpcion.Recurso(it) },
    )

    private val seccion = CampoSeccion(Ids.SECCION, R.string.campo_seccion, listOf(R.string.ayuda_seccion))

    private val paralelos = CampoOpciones(
        Ids.PARALELOS, R.string.campo_paralelos, listOf(R.string.ayuda_paralelos),
        opciones = (1..8).map { TextoOpcion.Literal(it.toString()) },
    )

    private val longitud = CampoNumerico(
        Ids.LONGITUD, R.string.campo_longitud, listOf(R.string.ayuda_longitud),
        unidades = { Unidades.longitud }, porDefecto = { if (it.longitudEnPies) "ft" else "m" }, hint = { "50" },
    )

    private val longitudTotal = CampoNumerico(
        Ids.LONGITUD, R.string.campo_longitud_total, listOf(R.string.ayuda_longitud_total),
        unidades = { Unidades.longitud }, porDefecto = { if (it.longitudEnPies) "ft" else "m" }, hint = { "100" },
    )

    private val material = CampoOpciones(
        Ids.MATERIAL, R.string.campo_material, listOf(R.string.ayuda_material),
        opciones = listOf(TextoOpcion.Recurso(R.string.material_cobre), TextoOpcion.Recurso(R.string.material_aluminio)),
    )

    private val tipoCable = CampoOpciones(
        Ids.TIPO_CABLE, R.string.campo_tipo_cable, listOf(R.string.ayuda_tipo_cable),
        opciones = listOf(TextoOpcion.Recurso(R.string.cable_unipolar), TextoOpcion.Recurso(R.string.cable_multipolar)),
    )

    private val temperatura = CampoNumerico(
        Ids.TEMPERATURA, R.string.campo_temperatura,
        listOf(R.string.ayuda_temperatura, R.string.ayuda_temperatura_valores),
        unidades = { Unidades.temperatura }, porDefecto = { if (it.temperaturaEnFahrenheit) "°F" else "°C" },
        hint = { "70" },
        inicial = { _, p -> if (p.temperaturaEnFahrenheit) "158" else "70" },
    )

    /** El cosφ solo se pide en alterna. */
    private val soloEnAlterna: (Campo, SistemaElectrico, Variante?, Map<String, Unidad>) -> Boolean =
        { campo, sistema, _, _ -> campo.id != Ids.FASE || sistema.esAlterna }

    private fun cosPhi(e: Entrada) = if (e.sistema.esAlterna) e.valor(Ids.FASE) else 1.0

    /** Corriente de la carga: directa si viene en amperes, o desde la potencia con la tensión y el cosφ. */
    private fun corriente(e: Entrada): Double {
        val unidad = e.unidad(Ids.CARGA)
        val valor = e.valor(Ids.CARGA)
        if (unidad.magnitud == Magnitud.CORRIENTE) return valor
        return LeyOhm.calcular(
            LeyOhm.Datos(e.sistema, mapOf(Magnitud.TENSION to e.valor(Ids.TENSION), unidad.magnitud!! to valor), cosPhi(e)),
            Magnitud.CORRIENTE,
        )
    }

    private fun cable(e: Entrada, longitudM: Double): Cable {
        val indice = e.opcion(Ids.SECCION)
        return Cable(
            seccion = if (e.unidad(Ids.SECCION).simbolo == AWG) Seccion.Awg(indice) else Seccion.Metrica(indice),
            material = if (e.opcion(Ids.MATERIAL) == 0) Material.COBRE else Material.ALUMINIO,
            tipo = if (e.opcion(Ids.TIPO_CABLE) == 0) TipoCable.UNIPOLAR else TipoCable.MULTIPOLAR,
            paralelos = e.opcion(Ids.PARALELOS) + 1,
            longitudM = longitudM,
            temperatura = e.valor(Ids.TEMPERATURA),
        )
    }

    /** Longitud en la unidad que eligió el usuario, para mostrarla igual que se ingresó. */
    private fun enUnidadElegida(e: Entrada, metros: Double): String {
        val unidad = e.unidad(Ids.LONGITUD)
        return "${Formato.numero(metros / unidad.aBase(1.0), 2)} ${unidad.simbolo}"
    }

    /** Área de la sección elegida en mm²; los calibres AWG y kcmil se convierten por su definición. */
    private fun areaMm2(e: Entrada): Double {
        val indice = e.opcion(Ids.SECCION)
        if (e.unidad(Ids.SECCION).simbolo != AWG) return TablasConductores.SECCIONES_MM2[indice]
        val nombre = TablasConductores.CALIBRES_AWG[indice]
        if (nombre.endsWith(" kcmil")) return nombre.removeSuffix(" kcmil").toDouble() * 0.506707479
        val calibre = nombre.removeSuffix(" AWG").let { if (it.endsWith("/0")) 1 - it.removeSuffix("/0").toInt() else it.toInt() }
        val diametro = 0.127 * 92.0.pow((36 - calibre) / 39.0)
        return PI / 4 * diametro * diametro
    }

    private fun verificarSeccion(e: Entrada): Verificacion = VerificacionRic.seccion(areaMm2(e), usos[e.opcion(Ids.USO)])

    private fun caidaDe(e: Entrada, cargas: Int = 1) =
        CaidaTension.calcular(e.sistema, e.valor(Ids.TENSION), corriente(e), cosPhi(e), cable(e, e.valor(Ids.LONGITUD)), cargas)

    /** Número de cargas: entero y al menos uno. */
    private fun cantidadCargas(e: Entrada): Int {
        val n = e.valor(Ids.NUMERO_CARGAS)
        if (n < 1 || n % 1.0 != 0.0) throw ErrorCalculo(MotivoError.VALOR_FUERA_DE_RANGO)
        return n.toInt()
    }

    val caida = Calculadora(
        id = "CalcoloCaduta",
        sistemas = SistemaElectrico.entries,
        campos = listOf(
            Campos.tension, carga, Campos.cosPhi, seccion, paralelos, longitud, material, tipoCable, temperatura, uso,
        ),
        visible = soloEnAlterna,
        guia = Guia(listOf(R.string.intro_caida, R.string.intro_caida_norma)),
        calcular = { e ->
            val r = caidaDe(e)
            listOf(
                LineaResultado(R.string.res_caida, Formato.conPrefijo(r.caidaV, "V")),
                LineaResultado(R.string.res_caida_porcentaje, "${Formato.fijo(r.porcentaje)} %"),
                LineaResultado(R.string.res_tension_carga, Formato.conPrefijo(r.tensionFinal, "V")),
                LineaResultado(R.string.res_corriente_linea, Formato.conPrefijo(corriente(e), "A"), principal = false),
            )
        },
        verificar = { e -> VerificacionRic.caida(caidaDe(e).porcentaje) + verificarSeccion(e) },
    )

    /** n cargas iguales repartidas a la misma distancia; la última está al final de la línea. */
    val cargasDistribuidas = Calculadora(
        id = "CalcoloCadutaCarichiDistribuiti",
        sistemas = SistemaElectrico.entries,
        campos = listOf(
            Campos.tension, cargaUnitaria, numeroCargas, Campos.cosPhi, seccion, paralelos, longitudTotal, material,
            tipoCable, temperatura, uso,
        ),
        visible = soloEnAlterna,
        guia = Guia(listOf(R.string.intro_caida_distribuidas, R.string.intro_caida_norma)),
        calcular = { e ->
            val cargas = cantidadCargas(e)
            val i = corriente(e)
            val r = caidaDe(e, cargas)
            listOf(
                LineaResultado(R.string.res_caida, Formato.conPrefijo(r.caidaV, "V")),
                LineaResultado(R.string.res_caida_porcentaje, "${Formato.fijo(r.porcentaje)} %"),
                LineaResultado(R.string.res_tension_ultima_carga, Formato.conPrefijo(r.tensionFinal, "V")),
                LineaResultado(R.string.res_distancia_cargas, enUnidadElegida(e, e.valor(Ids.LONGITUD) / cargas), principal = false),
                LineaResultado(R.string.res_corriente_cada_carga, Formato.conPrefijo(i, "A"), principal = false),
                LineaResultado(R.string.res_corriente_inicio, Formato.conPrefijo(i * cargas, "A"), principal = false),
            )
        },
        verificar = { e -> VerificacionRic.caida(caidaDe(e, cantidadCargas(e)).porcentaje) + verificarSeccion(e) },
    )

    /** Caída máxima ingresada, en V (se acepta en % de la tensión o directamente en volts). */
    private fun caidaMaximaV(e: Entrada): Double {
        val maxima = e.valor(Ids.CAIDA_MAXIMA)
        return if (e.unidad(Ids.CAIDA_MAXIMA).simbolo == PORCENTAJE) e.valor(Ids.TENSION) * maxima / 100.0 else maxima
    }

    /** Longitud para no superar la caída máxima, con la carga al final de la línea. */
    val longitudMaxima = Calculadora(
        id = "LunghezzaMassimaCavoCaduta",
        sistemas = SistemaElectrico.entries,
        campos = listOf(
            Campos.tension, carga, Campos.cosPhi, seccion, paralelos, caidaMaxima, material, tipoCable, temperatura, uso,
        ),
        visible = soloEnAlterna,
        guia = Guia(listOf(R.string.intro_longitud_maxima, R.string.intro_caida_norma)),
        calcular = { e ->
            val caidaMaxV = caidaMaximaV(e)
            val i = corriente(e)
            // La longitud del cable no interviene: se despeja de la caída admitida.
            val metros = CaidaTension.longitudMaxima(e.sistema, e.valor(Ids.TENSION), i, cosPhi(e), cable(e, 1.0), caidaMaxV)
            listOf(
                LineaResultado(R.string.res_longitud_maxima, "${Formato.numero(metros, 2)} m"),
                LineaResultado(R.string.res_longitud_maxima_pies, "${Formato.numero(metros / 0.3048, 2)} ft", principal = false),
                LineaResultado(R.string.res_caida_admitida, Formato.conPrefijo(caidaMaxV, "V"), principal = false),
                LineaResultado(R.string.res_corriente_linea, Formato.conPrefijo(i, "A"), principal = false),
            )
        },
        // Se verifica la caída que el usuario está admitiendo, no un resultado.
        verificar = { e ->
            VerificacionRic.caida(caidaMaximaV(e) * 100.0 / e.valor(Ids.TENSION)) + verificarSeccion(e)
        },
    )
}
