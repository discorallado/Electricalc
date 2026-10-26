package cl.electricalc.app.datos

import cl.electricalc.datos.ric.CapacidadCorregida
import cl.electricalc.datos.ric.CapacidadRic
import cl.electricalc.datos.ric.EmpalmeNormalizado
import cl.electricalc.datos.ric.MotorGrupo
import cl.electricalc.datos.ric.Ric
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.sqrt

/** Tipo de carga del cuadro (RIC N°03, numeral 6.1; RIC N°10: alumbrado = iluminación y enchufes). */
enum class TipoCarga { ILUMINACION, ENCHUFES, FUERZA, CLIMATIZACION }

/** Una fila del cuadro de cargas: [cantidad] consumos iguales de [potenciaW] cada uno. */
data class Carga(val nombre: String, val tipo: TipoCarga, val potenciaW: Double, val cantidad: Int = 1) {
    val totalW: Double get() = potenciaW * cantidad
}

/**
 * Proyecto guardado: datos comunes y cuadro de cargas. El resultado de cada etapa (demanda, empalme,
 * alimentador) se calcula a partir de estos datos, sin volver a escribirlos.
 */
data class Proyecto(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    /** Tipo de consumidor de la tabla N°3.1 del RIC N°03 (p. ej. "Casa habitación"). */
    val consumidor: String = "Casa habitación",
    val trifasico: Boolean = false,
    val tensionV: Double = 220.0,
    val cosPhi: Double = 1.0,
    val esVivienda: Boolean = true,
    val cargas: List<Carga> = emptyList(),
) {
    fun aJson(): JSONObject = JSONObject()
        .put("id", id).put("nombre", nombre).put("consumidor", consumidor).put("trifasico", trifasico)
        .put("tension", tensionV).put("cos_phi", cosPhi).put("vivienda", esVivienda)
        .put("cargas", JSONArray(cargas.map {
            JSONObject().put("nombre", it.nombre).put("tipo", it.tipo.name).put("potencia", it.potenciaW).put("cantidad", it.cantidad)
        }))

    companion object {
        fun deJson(o: JSONObject): Proyecto {
            val cargas = o.optJSONArray("cargas") ?: JSONArray()
            return Proyecto(
                id = o.getString("id"),
                nombre = o.getString("nombre"),
                consumidor = o.optString("consumidor", "Casa habitación"),
                trifasico = o.optBoolean("trifasico"),
                tensionV = o.optDouble("tension", 220.0),
                cosPhi = o.optDouble("cos_phi", 1.0),
                esVivienda = o.optBoolean("vivienda", true),
                cargas = (0 until cargas.length()).map { i ->
                    val c = cargas.getJSONObject(i)
                    Carga(c.getString("nombre"), TipoCarga.valueOf(c.getString("tipo")), c.getDouble("potencia"), c.optInt("cantidad", 1))
                },
            )
        }
    }
}

/** Resultado de las etapas 1 (cargas), 2 (empalme) y 4 (alimentador) de un proyecto. */
data class ResultadoProyecto(
    val instaladaW: Double,
    val alumbradoW: Double,
    val fuerzaW: Double,
    /** Demanda máxima: alumbrado con la tabla N°3.1 más fuerza y climatización con factor 1. */
    val demandaW: Double,
    /** Corriente de diseño del alimentador: alumbrado con su demanda; motores con 1,25 × el mayor (RIC N°07, 5.5.5). */
    val corrienteA: Double,
    val empalme: EmpalmeNormalizado?,
    /** Sección mínima del alimentador por capacidad (70 °C, método B1, 30 °C) y no menor a 4 mm². */
    val alimentador: CapacidadCorregida?,
    val calculosMemoria: List<String>,
    val memoriaObligatoria: Boolean,
)

object CalculoProyecto {
    fun calcular(p: Proyecto): ResultadoProyecto {
        val alumbrado = p.cargas.filter { it.tipo == TipoCarga.ILUMINACION || it.tipo == TipoCarga.ENCHUFES }.sumOf { it.totalW }
        val fuerzaCargas = p.cargas.filter { it.tipo == TipoCarga.FUERZA || it.tipo == TipoCarga.CLIMATIZACION }
        val fuerza = fuerzaCargas.sumOf { it.totalW }
        val instalada = alumbrado + fuerza
        val demandaAlumbrado = Ric.factoresDemanda.demanda(p.consumidor, alumbrado / 1000) * 1000
        val demanda = demandaAlumbrado + fuerza
        val k = if (p.trifasico) sqrt(3.0) else 1.0
        fun corriente(w: Double) = w / (k * p.tensionV * p.cosPhi)
        // Cada consumo de fuerza o climatización se trata como un motor permanente del grupo.
        val motores = fuerzaCargas.flatMap { c -> List(c.cantidad) { MotorGrupo(corriente(c.potenciaW)) } }
        val iFuerza = if (motores.isEmpty()) 0.0 else Ric.motores.corrienteGrupo(motores)
        val i = corriente(demandaAlumbrado) + iFuerza
        val minimoEmpalme = if (p.esVivienda) Ric.vivienda.empalmeProteccionMinA else 0.0
        val empalme = Ric.empalmes.para(demanda / 1000, p.trifasico, minimoEmpalme)
        val alimentador = CapacidadRic.seccionPara(
            i, 70, "B1", 30.0, if (p.trifasico) 3 else 2, Ric.seccionesMinimas.minima("alimentador")!!.mm2,
        )
        val r = Ric.proyecto
        return ResultadoProyecto(
            instaladaW = instalada, alumbradoW = alumbrado, fuerzaW = fuerza, demandaW = demanda, corrienteA = i,
            empalme = empalme, alimentador = alimentador,
            calculosMemoria = r.calculosPara(instalada / 1000),
            memoriaObligatoria = instalada / 1000 > r.memoriaObligatoriaSobreKw,
        )
    }
}
