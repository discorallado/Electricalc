package cl.electricalc.calc.conductores

/**
 * Material del conductor.
 *
 * @param resistividad20 Ω·mm²/m a 20 °C.
 * @param alfa20 coeficiente de temperatura referido a 20 °C (tablas métricas).
 * @param alfa75 coeficiente de temperatura referido a 75 °C (tablas AWG, NEC cap. 9).
 */
enum class Material(val resistividad20: Double, val alfa20: Double, val alfa75: Double) {
    COBRE(0.01851, 0.00393, 0.00323),
    ALUMINIO(0.02941, 0.00403, 0.0033),
}

/** Disposición del cable; afecta la reactancia de las secciones métricas. */
enum class TipoCable { UNIPOLAR, MULTIPOLAR }

/** Sección normalizada: métrica (mm²) o AWG/kcmil. [indice] apunta a las tablas. */
sealed interface Seccion {
    val indice: Int
    val nombre: String

    data class Metrica(override val indice: Int) : Seccion {
        init {
            require(indice in TablasConductores.SECCIONES_MM2.indices) { "Sección métrica fuera de tabla: $indice" }
        }
        val mm2: Double get() = TablasConductores.SECCIONES_MM2[indice]
        override val nombre: String get() = "${formatearMm2(mm2)} mm²"
    }

    data class Awg(override val indice: Int) : Seccion {
        init {
            require(indice in TablasConductores.CALIBRES_AWG.indices) { "Calibre AWG fuera de tabla: $indice" }
        }
        override val nombre: String get() = TablasConductores.CALIBRES_AWG[indice]
    }

    companion object {
        fun mm2(valor: Double): Metrica = Metrica(
            TablasConductores.SECCIONES_MM2.indexOf(valor).also {
                require(it >= 0) { "Sección no normalizada: $valor mm²" }
            },
        )

        fun awg(nombre: String): Awg = Awg(
            TablasConductores.CALIBRES_AWG.indexOf(nombre).also {
                require(it >= 0) { "Calibre no reconocido: $nombre" }
            },
        )
    }
}

private fun formatearMm2(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

/** Cable de una línea: sección, material, disposición, conductores en paralelo, longitud y temperatura. */
data class Cable(
    val seccion: Seccion,
    val material: Material = Material.COBRE,
    val tipo: TipoCable = TipoCable.MULTIPOLAR,
    val paralelos: Int = 1,
    /** Longitud de la línea en metros (un sentido). */
    val longitudM: Double,
    /** Temperatura de servicio del conductor en °C. */
    val temperatura: Double = 70.0,
) {
    init {
        require(paralelos >= 1) { "Conductores en paralelo debe ser ≥ 1" }
    }

    /** Resistencia en Ω/km a la temperatura de servicio. */
    fun resistenciaOhmKm(alterna: Boolean): Double {
        val (base, alfa, tRef) = when (seccion) {
            is Seccion.Metrica -> Triple(TablasConductores.resistenciaMetrica(seccion.indice, material, alterna), material.alfa20, 20.0)
            is Seccion.Awg -> Triple(TablasConductores.resistenciaAwg(seccion.indice, material, alterna), material.alfa75, 75.0)
        }
        return base * (1.0 + alfa * (temperatura - tRef))
    }

    /** Reactancia en Ω/km a la frecuencia de las tablas (50 Hz métricas, 60 Hz AWG). */
    fun reactanciaOhmKm(): Double = when (seccion) {
        is Seccion.Metrica -> TablasConductores.reactanciaMetrica(seccion.indice, tipo)
        is Seccion.Awg -> TablasConductores.REACTANCIA_AWG[seccion.indice]
    }
}
