package cl.electricalc.calc.conductores

/**
 * Tablas de resistencia y reactancia por sección.
 *
 * Métricas: Ω/km a 20 °C (CC según IEC 60228; CA incluye efecto pelicular), reactancia a 50 Hz.
 * AWG/kcmil: Ω/km a 75 °C y reactancia a 60 Hz (NEC, capítulo 9, tablas 8 y 9).
 */
object TablasConductores {

    val SECCIONES_MM2 = listOf(
        0.5, 0.75, 1.0, 1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0, 50.0, 70.0, 95.0, 120.0, 150.0,
        185.0, 240.0, 300.0, 400.0, 500.0, 630.0, 800.0, 1000.0,
    )

    val CALIBRES_AWG = listOf(
        "28 AWG", "26 AWG", "24 AWG", "22 AWG", "20 AWG", "18 AWG", "16 AWG", "14 AWG", "12 AWG", "10 AWG",
        "8 AWG", "6 AWG", "4 AWG", "3 AWG", "2 AWG", "1 AWG", "1/0 AWG", "2/0 AWG", "3/0 AWG", "4/0 AWG",
        "250 kcmil", "300 kcmil", "350 kcmil", "400 kcmil", "500 kcmil", "600 kcmil", "700 kcmil", "750 kcmil",
        "800 kcmil", "900 kcmil", "1000 kcmil", "1250 kcmil", "1500 kcmil", "1750 kcmil", "2000 kcmil",
    )

    private val R_CU_CC_MM2 = doubleArrayOf(
        39.0, 26.0, 19.5, 13.3, 7.98, 4.95, 3.3, 1.91, 1.21, 0.78, 0.554, 0.386, 0.272, 0.206, 0.161, 0.129,
        0.106, 0.0801, 0.0641, 0.0486, 0.0384, 0.0287, 0.0221, 0.0176,
    )
    private val R_AL_CC_MM2 = doubleArrayOf(
        56.0, 38.0, 28.0, 18.1, 12.1, 7.41, 4.61, 3.08, 1.91, 1.2, 0.868, 0.641, 0.443, 0.32, 0.253, 0.206,
        0.164, 0.125, 0.1, 0.0778, 0.0605, 0.0469, 0.0367, 0.0291,
    )
    private val R_CU_CA_MM2 = doubleArrayOf(
        39.0, 26.0, 19.5, 13.3, 7.98, 4.95, 3.3, 1.91, 1.21, 0.78, 0.554, 0.387, 0.273, 0.206, 0.164, 0.133,
        0.109, 0.0863, 0.0667, 0.0525, 0.0416, 0.0337, 0.0306, 0.0296,
    )
    private val R_AL_CA_MM2 = doubleArrayOf(
        56.0, 38.0, 28.0, 18.1, 12.1, 7.41, 4.61, 3.08, 1.91, 1.2, 0.868, 0.641, 0.443, 0.32, 0.253, 0.206,
        0.164, 0.126, 0.101, 0.0786, 0.0624, 0.0497, 0.0416, 0.0372,
    )
    private val X_UNIPOLAR_MM2 = doubleArrayOf(
        0.171, 0.159, 0.152, 0.144, 0.132, 0.122, 0.114, 0.105, 0.098, 0.093, 0.089, 0.085, 0.084, 0.083, 0.08,
        0.08, 0.08, 0.078, 0.076, 0.076, 0.074, 0.073, 0.073, 0.073,
    )
    private val X_MULTIPOLAR_MM2 = doubleArrayOf(
        0.12, 0.112, 0.107, 0.1, 0.094, 0.087, 0.083, 0.078, 0.075, 0.074, 0.072, 0.071, 0.07, 0.069, 0.069,
        0.069, 0.069, 0.069, 0.068, 0.068, 0.068, 0.068, 0.068, 0.068,
    )

    private val R_CU_CA_AWG = doubleArrayOf(
        262.3, 165.0, 103.7, 65.2, 41.0, 25.8, 16.2, 10.2, 6.6, 3.9, 2.56, 1.61, 1.02, 0.82, 0.62, 0.49, 0.39,
        0.33, 0.253, 0.203, 0.171, 0.144, 0.125, 0.108, 0.089, 0.075, 0.066, 0.062, 0.06, 0.054, 0.049, 0.043,
        0.037, 0.032, 0.028,
    )
    private val R_AL_CA_AWG = doubleArrayOf(
        432.3, 271.9, 171.0, 107.5, 67.6, 42.5, 26.7, 16.8, 10.5, 6.6, 4.3, 2.66, 1.67, 1.31, 1.05, 0.82, 0.66,
        0.52, 0.43, 0.33, 0.279, 0.233, 0.2, 0.177, 0.141, 0.118, 0.103, 0.095, 0.091, 0.083, 0.075, 0.056,
        0.053, 0.05, 0.047,
    )
    private val R_CU_CC_AWG = doubleArrayOf(
        258.9, 162.8, 102.4, 64.4, 40.5, 25.5, 16.0, 10.1, 6.34, 3.984, 2.506, 1.608, 1.01, 0.802, 0.634, 0.505,
        0.399, 0.317, 0.2512, 0.1996, 0.1687, 0.1409, 0.1205, 0.1053, 0.0845, 0.0704, 0.0603, 0.0563, 0.0528,
        0.047, 0.0423, 0.0338, 0.02814, 0.0241, 0.02109,
    )
    private val R_AL_CC_AWG = doubleArrayOf(
        426.8, 268.4, 168.8, 106.2, 66.8, 42.0, 26.4, 16.6, 10.45, 6.561, 4.125, 2.652, 1.666, 1.32, 1.045,
        0.829, 0.66, 0.523, 0.413, 0.328, 0.2778, 0.2318, 0.1984, 0.1737, 0.1391, 0.1159, 0.0994, 0.0927,
        0.0868, 0.077, 0.0695, 0.0554, 0.0464, 0.0397, 0.0348,
    )
    val REACTANCIA_AWG: List<Double> = listOf(
        0.253, 0.246, 0.239, 0.232, 0.225, 0.218, 0.203, 0.19, 0.177, 0.164, 0.171, 0.167, 0.157, 0.154, 0.148,
        0.151, 0.144, 0.141, 0.138, 0.135, 0.135, 0.135, 0.131, 0.131, 0.128, 0.128, 0.126, 0.125, 0.124, 0.122,
        0.121, 0.118, 0.116, 0.115, 0.114,
    )

    fun resistenciaMetrica(indice: Int, material: Material, alterna: Boolean): Double = when (material) {
        Material.COBRE -> if (alterna) R_CU_CA_MM2 else R_CU_CC_MM2
        Material.ALUMINIO -> if (alterna) R_AL_CA_MM2 else R_AL_CC_MM2
    }[indice]

    fun resistenciaAwg(indice: Int, material: Material, alterna: Boolean): Double = when (material) {
        Material.COBRE -> if (alterna) R_CU_CA_AWG else R_CU_CC_AWG
        Material.ALUMINIO -> if (alterna) R_AL_CA_AWG else R_AL_CC_AWG
    }[indice]

    fun reactanciaMetrica(indice: Int, tipo: TipoCable): Double = when (tipo) {
        TipoCable.UNIPOLAR -> X_UNIPOLAR_MM2
        TipoCable.MULTIPOLAR -> X_MULTIPOLAR_MM2
    }[indice]
}
