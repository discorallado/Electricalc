package cl.electricalc.app.ui.calculadoras

/** Calculadoras implementadas, por id del catálogo. Las demás herramientas abren "Próximamente". */
object Registro {
    val todas = CalculadorasOhm.todas +
        listOf(CalculadoraCaida.caida, CalculadoraCaida.cargasDistribuidas, CalculadoraCaida.longitudMaxima) +
        CalculadorasTierra.todas

    private val porId = todas.associateBy { it.id }

    fun calculadora(id: String): Calculadora? = porId[id]

    fun implementada(id: String) = id in porId
}
