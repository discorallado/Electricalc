package cl.electricalc.app.ui.calculadoras

/** Calculadoras implementadas, por id del catálogo. Las demás herramientas abren "Próximamente". */
object Registro {
    val todas = CalculadorasOhm.todas +
        listOf(CalculadoraCaida.caida, CalculadoraCaida.cargasDistribuidas, CalculadoraCaida.longitudMaxima) +
        CalculadorasTierra.todas + CalculadorasRic.todas + CalculadorasProteccion.todas +
        CalculadorasEmpalme.todas + CalculadoraDuctos.ductos +
        CalculadorasEnergia.todas + CalculadoraPuestaEnServicio.protocolo + CalculadoraTablero.tablero + CalculadoraAutogeneracion.fv + CalculadoraVolumenesBano.volumenes + CalculadoraImpedanciaBucle.bucle + CalculadoraTransformador.transformador + CalculadoraAreasPeligrosas.areas + CalculadoraMantenimiento.plan +
        CalculadorasMotor.todas + CalculadorasElectronica.todas + CalculadorasConversion.todas +
        CalculadorasConductor.todas + CalculadorasCortocircuito.todas + CalculadorasRespaldo.todas + CalculadorasColores.todas + CalculadoraSmd.smd + CalculadoraCondensador.codigo

    private val porId = todas.associateBy { it.id }

    fun calculadora(id: String): Calculadora? = porId[id]

    fun implementada(id: String) = id in porId
}
