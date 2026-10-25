package cl.electricalc.app.ui.calculadoras

/** Calculadoras implementadas, por id del catálogo. Las demás herramientas abren "Próximamente". */
object Registro {
    private val porId = (CalculadorasOhm.todas + CalculadoraCaida.caida).associateBy { it.id }

    fun calculadora(id: String): Calculadora? = porId[id]

    fun implementada(id: String) = id in porId
}
