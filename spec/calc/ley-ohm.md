# Ley de Ohm y potencias (Ola 1)

Pantallas de referencia: cálculo de corriente, tensión, resistencia, impedancia, potencia
activa, aparente y reactiva, y factor de potencia (`FragmentCalcolo*`, base
`GeneralFragmentLeggeOhm`).

## Convención

| Magnitud | Alterna (k = 1 mono/bifásica, √3 trifásica, V de línea) | Continua |
|---|---|---|
| Z | V / I | = R |
| R | Z · cosφ | V / I |
| S | k · V · I | V · I |
| P | S · cosφ | V · I |
| Q | S · senφ | no aplica |

## Entradas

Cualquier combinación de magnitudes conocidas más cosφ. Si los datos ya fijan el cosφ
(dos potencias, o R y Z), el cosφ ingresado se ignora. Cada valor debe ser > 0 y el cosφ
debe estar en (0, 1].

La referencia ofrece estas combinaciones en sus selectores (p. ej. corriente: V+P, V+R,
V+Z, P+R, P+Z). Nuestro motor acepta cualquiera que determine la salida.

## Validación

`LeyOhmGoldenTest`: 3.028 vectores generados ejecutando el motor de la referencia en un
arnés JVM. Coinciden los 2.016 casos con valor y los rechazos por valor inválido. Nuestro
motor resuelve además algunas combinaciones que la referencia no soporta (p. ej. Z en
continua).
