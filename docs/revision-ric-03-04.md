# Revisión de las tablas transcritas de los RIC N°03 y N°04

Regla de `docs/arquitectura-v2.md` (sección 7): cada valor normativo lo revisa una persona contra el
PDF oficial antes del merge. Esta lista dice qué mirar y dónde.

- PDF: `%USERPROFILE%\.cache\electricalc-ric\RIC-03.pdf` y `RIC-04.pdf`.
- Versión de ambos: Resolución Exenta N°33.877 del 30/12/2020. El N°03 trae una nota: el texto del
  numeral 5.1.3 se agregó en la versión del PDF publicada el 12.01.2021.
- Las páginas son las impresas en el pie ("Página N | 90"), no la numeración del visor.
- Extracción: `pdftotext -table`, que mantiene las columnas alineadas. La extracción con `-layout`
  desplaza filas en la tabla 4.4 y desordena la 3.2: **no usarla para transcribir**.
- Al terminar de revisar un archivo, cambia `"revision": { "estado": "pendiente" }` por `"revisada"`.
  La app deja de mostrar el aviso "en revisión" para esa tabla.

Archivos en `core-data/src/main/resources/ric/`.

## RIC N°03

| Archivo | Fuente | Qué revisar |
|---|---|---|
| `ric-03/caidas-admisibles.json` | Numeral 5.1.3, p. 3 | 3 % por tramo (alimentadores, subalimentadores y conductores) y 5 % total en el punto más desfavorable. |
| `ric-03/factores-demanda-alumbrado.json` | Tabla 3.1, p. 5 | 7 tipos de consumidor y sus tramos. Los tramos se guardan como límite superior acumulado ("Primeros 3", "Desde 3 a 120", "Sobre 120"). Hoteles dice "Desde 20,1 a 100". |
| `ric-03/factores-simultaneidad-viviendas.json` | Tabla 3.2, p. 6 | 21 factores para n = 1…21 (1; 1; 1; 0,95 … 0,73) y la expresión (15,3 + (n − 21) × 0,5)/n para n > 21. |

## RIC N°04

| Archivo | Fuente | Qué revisar |
|---|---|---|
| `ric-04/secciones-minimas.json` | Numeral 5.4, p. 6 | Iluminación 1,5; enchufes 2,5; mixtos 2,5; subalimentadores 2,5; alimentadores 4,0 mm². Coincide con el numeral 5.1.2 del RIC N°03. |
| `ric-04/colores-conductores.json` | Numeral 5.32, p. 9 | Fase 1 azul, fase 2 negro, fase 3 rojo, neutro y tierra de servicio blanco, protección verde o verde/amarillo. |
| `ric-04/capacidad-cobre-aislado.json` | Tabla 4.4, pp. 18–20 | 34 secciones × 3 métodos a 70 °C y × 8 métodos a 90 °C. Temperatura ambiente 30 °C, salvo D1 y D2, que van a 20 °C. Puntos marcados (abajo). |
| `ric-04/capacidad-uso-movil.json` | Tabla 4.5, pp. 21–22 | 37 secciones, método E a 30 °C. 1,5 mm² = 18,5 A. |
| `ric-04/factor-cantidad-conductores.json` | Tabla 4.6, p. 23 | 4–6: 0,8; 7–24: 0,7; 25–42: 0,6; sobre 42: 0,5. |
| `ric-04/factor-temperatura.json` | Tabla 4.7, p. 23 | 11 rangos (5–10 a 56–60 °C) × 3 columnas: 70 °C (A1, B1, E), 90 °C (A1, A2, B1, B2, E) y 90 °C (D1, D2). |

### Puntos a mirar con cuidado en la tabla 4.4

1. **90 °C, método D2:** salto de 39 A (3,31 mm²) a 59 A (4 mm²).
2. **90 °C, métodos B1 y B2:** 2,08 mm² y 2,5 mm² tienen el mismo valor (24 A).
3. **90 °C, método E:** 1,5 mm² = 19 A, igual que a 70 °C, y 2,08 mm² = 28 A.
4. **90 °C, método F:** guion bajo 4 mm²; se guarda sin valor.
5. **Tabla 4.7:** el encabezado de la columna D1/D2 dice "D2*". La nota del asterisco no aparece en
   el texto extraído; búscala en la página 23.

### Controles automáticos (`RicTest` en `core-data`)

No reemplazan la revisión, pero detectan los errores típicos de transcripción:

- Ninguna columna de capacidad baja al aumentar la sección, y las secciones van en orden.
- La tabla 4.5 coincide valor a valor con el método E a 70 °C de la tabla 4.4 desde 25 mm². Son dos
  tablas distintas del PDF, así que si las filas estuvieran corridas no calzarían.
- ft vale 1 a 30 °C (y a 20 °C en D1/D2) y baja siempre con la temperatura.
- La tabla 3.2 empalma con su expresión en n = 21.
