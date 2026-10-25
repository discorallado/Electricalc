package cl.electricalc.app.ui.calculadoras

import cl.electricalc.app.R
import cl.electricalc.calc.caida.CaidaTension
import cl.electricalc.calc.comun.SistemaElectrico
import cl.electricalc.calc.conductores.Cable
import cl.electricalc.calc.conductores.Material
import cl.electricalc.calc.conductores.Seccion
import cl.electricalc.calc.conductores.TipoCable
import cl.electricalc.calc.ohm.LeyOhm
import cl.electricalc.calc.ohm.Magnitud

/** Caída de tensión en una línea con la carga al final ([CaidaTension]). */
object CalculadoraCaida {
    /** Unidades del campo de sección; el índice de la opción apunta a la tabla de cada una. */
    const val MM2 = "mm²"
    const val AWG = "AWG"

    private val carga = CampoNumerico(
        Ids.CARGA, R.string.campo_carga, listOf(R.string.ayuda_carga, R.string.ayuda_carga_unidades),
        unidades = Unidades::carga, porDefecto = { "W" }, hint = { "3000" },
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

    val caida = Calculadora(
        id = "CalcoloCaduta",
        sistemas = SistemaElectrico.entries,
        campos = listOf(
            Campos.tension, carga, Campos.cosPhi, seccion, paralelos, longitud, material, tipoCable, temperatura,
        ),
        visible = { campo, sistema, _, _ -> campo.id != Ids.FASE || sistema.esAlterna },
        guia = Guia(listOf(R.string.intro_caida, R.string.intro_caida_norma)),
        calcular = { e ->
            val tension = e.valor(Ids.TENSION)
            val cos = if (e.sistema.esAlterna) e.valor(Ids.FASE) else 1.0
            val unidadCarga = e.unidad(Ids.CARGA)
            val valorCarga = e.valor(Ids.CARGA)
            val corriente = if (unidadCarga.magnitud == Magnitud.CORRIENTE) {
                valorCarga
            } else {
                LeyOhm.calcular(
                    LeyOhm.Datos(e.sistema, mapOf(Magnitud.TENSION to tension, unidadCarga.magnitud!! to valorCarga), cos),
                    Magnitud.CORRIENTE,
                )
            }
            val indice = e.opcion(Ids.SECCION)
            val cable = Cable(
                seccion = if (e.unidad(Ids.SECCION).simbolo == AWG) Seccion.Awg(indice) else Seccion.Metrica(indice),
                material = if (e.opcion(Ids.MATERIAL) == 0) Material.COBRE else Material.ALUMINIO,
                tipo = if (e.opcion(Ids.TIPO_CABLE) == 0) TipoCable.UNIPOLAR else TipoCable.MULTIPOLAR,
                paralelos = e.opcion(Ids.PARALELOS) + 1,
                longitudM = e.valor(Ids.LONGITUD),
                temperatura = e.valor(Ids.TEMPERATURA),
            )
            val r = CaidaTension.calcular(e.sistema, tension, corriente, cos, cable)
            listOf(
                LineaResultado(R.string.res_caida, Formato.conPrefijo(r.caidaV, "V")),
                LineaResultado(R.string.res_caida_porcentaje, "${Formato.fijo(r.porcentaje)} %"),
                LineaResultado(R.string.res_tension_carga, Formato.conPrefijo(r.tensionFinal, "V")),
                LineaResultado(R.string.res_corriente_linea, Formato.conPrefijo(corriente, "A"), principal = false),
            )
        },
    )
}
