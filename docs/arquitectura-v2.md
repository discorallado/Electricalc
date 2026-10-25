# Electricalc v2: arquitectura de información orientada a proyectos en Chile

Destino en el repo: `docs/arquitectura-v2.md`, junto a `spec/catalogo-v2.json`.
Reemplaza la sección 3.1 (navegación) de `docs/HANDOFF.md`. La anatomía de las pantallas de
cálculo (3.2 a 3.5) se mantiene.

## 1. Por qué cambiar las pestañas

Las pestañas del APK (Principal, Motor, Conversiones, Recursos, Patillaje, Formulario)
ordenan por **tipo de contenido**. Quien trabaja en un proyecto busca por **problema**
("¿qué sección uso?", "¿cumple la caída?", "¿qué diferencial pongo?") y por **norma**
("¿qué exige el RIC N°06?"). La v2 ofrece tres entradas al mismo contenido:

1. **Por etapa del proyecto**: un flujo que lleva de las cargas a la declaración.
2. **Por problema**: categorías de cálculo con nombres de tarea.
3. **Por pliego RIC**: los 19 pliegos, cada uno con sus herramientas, tablas y esquemas.

Además hay un **buscador global** sobre todo el contenido (títulos, sinónimos y número de
pliego: buscar "RIC 4", "diferencial" o "caída" debe encontrar lo mismo que navegar).

## 2. Navegación

Barra inferior con 5 destinos (Material 3 `NavigationBar`):

| Destino | Contenido |
|---|---|
| **Inicio** | Buscador, recientes, favoritos y accesos "¿Qué estás haciendo?" (vivienda, local comercial, fotovoltaico, carga de VE, motor, puesta a tierra). |
| **Proyecto** | Proyectos guardados y el flujo por etapas (sección 3). |
| **Calcular** | Categorías por problema (sección 4). |
| **RIC** | Los 19 pliegos (sección 5). |
| **Referencias** | Tablas, Esquemas, Conectores y pinouts, Conversiones y unidades, Formulario. |

El menú lateral queda para ajustes (norma por defecto: RIC, IEC, NEC; unidades; idioma),
guía de uso y acerca de.

## 3. Proyecto: flujo por etapas

Un proyecto guarda datos compartidos (tipo de instalación, tensión, sistema de tierra) y los
cálculos de cada etapa. Cada etapa muestra sus herramientas en orden y un estado (pendiente,
calculado, cumple o no cumple RIC).

| Etapa (`etapa` en el catálogo) | Herramientas |
|---|---|
| 1. Cargas y demanda (`cargas`) | Cuadro de cargas, corriente de empleo, motores, compensación de FP, iluminación, consumo y costo |
| 2. Empalme (`empalme`) | Selección de empalme |
| 3. Tableros (`tableros`) | Dimensionamiento de tablero, capacidad de barras |
| 4. Alimentadores y circuitos (`alimentadores`) | Circuitos de vivienda, sección, capacidad RIC, caída con verificación, ductos y bandejas, volúmenes de baño |
| 5. Protecciones (`protecciones`) | Protección contra sobrecarga y cortocircuito, Icc, diferencial, SPD |
| 6. Puesta a tierra (`tierra`) | Wenner, electrodos y mallas, coordinación TT |
| 7. Sistemas (`sistemas`) | VE, fotovoltaico, respaldo, transformador y sala eléctrica, áreas peligrosas |
| 8. Documentación (`documentacion`) | Memoria de cálculo y checklist TE1, protocolo de puesta en servicio, plan de mantenimiento |

Los resultados de una etapa alimentan a la siguiente: por ejemplo, la demanda del cuadro de
cargas pasa a la corriente del empalme y del alimentador sin volver a escribirla.

## 4. Calcular: categorías por problema

`categoria` en el catálogo:

- **Fundamentos eléctricos**: ley de Ohm, potencias, FP, impedancia, reactancia, efecto Joule.
- **Conductores y canalizaciones**: sección, capacidad, caída, longitud máxima, temperatura,
  pérdidas, neutro, barras, ductos, cuadro de cargas, empalme, circuitos de vivienda.
- **Protecciones y cortocircuito**: dispositivo de protección, Icc, K²S², SPD, fusibles,
  diferencial, tablero.
- **Puesta a tierra y seguridad**: tierra, Wenner, electrodos, TT, volúmenes de baño, puesta en
  servicio.
- **Energía, FP y eficiencia**: compensación, condensadores, iluminación, consumo y costo.
- **Motores y transformadores**.
- **Sistemas**: emergencia, respaldo, baterías, fotovoltaico, VE, áreas peligrosas.
- **Electrónica e instrumentación**: códigos de componentes, divisores, LED, Zener,
  termistores, señales analógicas, CCTV.

Dentro de cada categoría, las herramientas con verificación RIC van primero.

## 5. RIC: los 19 pliegos

Lista de pliegos (número y título). Cada página de pliego tiene:

1. **De qué trata**: resumen con redacción propia, alcance y a quién aplica.
2. **Herramientas de este pliego**: `herramientas_existentes` y `herramientas_nuevas`.
3. **Tablas y límites**: los valores transcritos del pliego, con numeral.
4. **Esquemas** relacionados.
5. **Checklist** de los requisitos más revisados en inspección (cuando aplique).
6. **Enlace al PDF oficial** de la SEC y fecha de la versión transcrita.

En cada pantalla de cálculo, un **chip "RIC N°04"** en la barra (uno por pliego relacionado)
abre la página del pliego. Así la norma está a un toque desde cualquier cálculo.

Títulos usados (listado SEC, DS N°8/2019; confirmar con el PDF de cada uno, el N°15 tiene
versión 2024):

| N° | Pliego | N° | Pliego |
|---|---|---|---|
| 01 | Empalmes | 11 | Instalaciones especiales |
| 02 | Tableros eléctricos | 12 | Instalaciones en ambientes explosivos |
| 03 | Alimentadores y demanda de una instalación | 13 | Subestaciones y salas eléctricas |
| 04 | Conductores, materiales y sistemas de canalización | 14 | Eficiencia energética |
| 05 | Medidas de protección contra tensiones peligrosas y descargas eléctricas | 15 | Infraestructura para la recarga de vehículos eléctricos |
| 06 | Puesta a tierra y enlace equipotencial | 16 | Subsistemas de distribución |
| 07 | Instalaciones de equipos | 17 | Operación y mantenimiento |
| 08 | Sistemas de emergencia | 18 | Presentación de proyectos |
| 09 | Sistemas de autogeneración | 19 | Puesta en servicio |
| 10 | Instalaciones de uso general | | |

El RIC N°16 todavía no tiene herramientas asignadas: definir su alcance al leer el PDF.

## 6. Verificación RIC: patrón de UI

Toda herramienta con `datos_ric` muestra, bajo el resultado, una tarjeta de verificación:

- **Cumple** (verde), **No cumple** (rojo) o **Revisar** (ámbar, cuando falta un dato).
- El valor calculado frente al límite ("2,8 % ≤ 3 %").
- La referencia exacta ("RIC N°03, numeral X, tabla Y") y un enlace a la página del pliego.
- Si la tabla todavía no está transcrita: "Verificación RIC pendiente" en gris. Nunca se
  muestra un límite inventado o de memoria.

## 7. Datos del RIC: regla de oro

1. Descargar los PDF oficiales desde sec.cl a una carpeta **fuera del repo**, por ejemplo
   `%USERPROFILE%\.cache\electricalc-ric\RIC-04.pdf`.
2. Transcribir cada tabla a `core-data/ric/ric-04/<tabla>.json` con: pliego, versión, numeral,
   tabla, unidades, valores y notas.
3. Un test por tabla verifica forma y unidades. Los valores los revisa una persona contra el
   PDF antes del merge (checklist en el PR).
4. Los resúmenes de cada pliego se redactan con palabras propias; los valores normativos sí se
   citan tal cual, siempre con su referencia.

Cada entrada de `existentes` trae `ric_confianza`: **alta** cuando el tema es el objeto del
pliego y **media** cuando la relación es probable pero hay que confirmar el numeral en el PDF
antes de mostrar la referencia.

## 8. Herramientas nuevas (resumen)

36 en `nuevas` del catálogo: 16 calculadoras, 2 verificaciones, 1 asistente, 2 checklists,
1 documento, 6 tablas y 8 esquemas. Lo más valioso para un proyecto en Chile:

- **Cuadro de cargas → empalme → alimentadores → memoria de cálculo en PDF**: el hilo
  conductor de un proyecto real, y lo que se presenta en la declaración.
- **Verificación de caída y capacidad según RIC**, con semáforo.
- **Circuitos de vivienda**: sección, protección, puntos y diferencial por tipo de circuito.
- **Diferencial y puesta a tierra**: selección de diferencial, Wenner, electrodos, coordinación TT.
- **Electromovilidad y fotovoltaico**: carga de VE frente a la capacidad del empalme y
  Netbilling con strings y caída en CC.
- **Puesta en servicio**: protocolo de pruebas que genera un PDF.
- **Esquemas propios**: unilineal de vivienda, empalmes, TT/TN/IT animados, FV, VE y
  transferencia de respaldo.

## 9. Fases de implementación

| Fase | Contenido |
|---|---|
| A | Nueva navegación (5 destinos), buscador, catálogo v2, chips RIC, páginas de pliego con resumen y herramientas (sin tablas aún). Mover las pantallas existentes a sus categorías. |
| B | Datos RIC N°03 y N°04: capacidad, caídas admisibles, secciones mínimas y colores. Verificación con semáforo en caída y sección. |
| C | Prioridad 1 de `nuevas`: cuadro de cargas, empalme, circuitos de vivienda, ductos, diferencial, Wenner, TT, carga de VE y memoria de cálculo. Proyectos guardados con el flujo por etapas. |
| D | Prioridad 2 (FV, respaldo, tablero, iluminación, consumo, protocolo de puesta en servicio) y esquemas. |
| E | Prioridad 3 (transformador y sala, áreas peligrosas, mantenimiento) y el resto del catálogo. |

## 10. Prompt para Claude Code (fase A)

> Lee `docs/arquitectura-v2.md`, `spec/catalogo-v2.json` y `docs/HANDOFF.md`. Implementa la
> fase A: barra inferior con Inicio, Proyecto, Calcular, RIC y Referencias; buscador global
> sobre títulos, sinónimos y número de pliego; categorías de Calcular según `categoria`;
> páginas de los 19 pliegos con herramientas relacionadas y un resumen propio marcado como
> borrador; chip "RIC N°xx" en cada calculadora que abre su pliego. Las pantallas que ya
> existen se mantienen y solo cambian de lugar. No muestres valores normativos: todavía no
> hay tablas RIC transcritas. Compila, prueba en el emulador y muéstrame capturas. No hagas
> commits.
