# Caída de tensión (Ola 2)

Pantalla de referencia: `FragmentCalcoloCaduta` (y `FragmentCalcoloCadutaCarichiDistribuiti`).

## Entradas

Sistema, tensión, carga (como I, P, S o Q; si no es corriente se convierte con `LeyOhm`),
cosφ, sección (mm² o AWG/kcmil), conductores en paralelo, longitud, material (Cu/Al),
tipo de cable (unipolar/multipolar) y temperatura de servicio.

## Fórmula

ΔV = f · I · L[km] · (R·cosφ + X·senφ) · (n + 1)/2 / paralelos

- f = 2 en CC, monofásica y bifásica; √3 en trifásica.
- R a la temperatura de servicio: R_T = R_ref · (1 + α (T − T_ref)). Métricas: T_ref = 20 °C;
  AWG: T_ref = 75 °C.
- X: tabla métrica a 50 Hz (unipolar/multipolar) o tabla AWG a 60 Hz. En CC, X = 0 y se usa
  la resistencia de CC.
- n = número de cargas iguales distribuidas (1 = carga al final).
- ΔV se acota a la tensión de alimentación. Salidas: ΔV, % y tensión final.

Longitud máxima: L = ΔV_max / (f · I · (R cosφ + X senφ)) · paralelos.

## Tablas

`core-calc/.../conductores/TablasConductores.kt`. Pendiente contrastar con IEC 60228 y NEC
cap. 9, tablas 8 y 9.

## Validación

`CaidaTensionGoldenTest`: 1.500 casos aleatorios generados con el código de referencia;
todos coinciden.
