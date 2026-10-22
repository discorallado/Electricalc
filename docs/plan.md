# Plan: extraer Electrical Calculations 11.2.1 y construir Electricalc

Objetivo: entender a fondo la app original (`it.Ettore.calcolielettrici`) con REA y
construir una app propia equivalente en funcionalidad, con código, textos y
diseño propios.

## Reglas de juego

- **Sí extraemos**: fórmulas, algoritmos, validaciones, constantes físicas, tablas
  técnicas (ampacidades, reactancias, secciones AWG, etc.), estructura de pantallas
  y flujos. Son hechos técnicos que reimplementamos con nuestro propio código.
- **No copiamos**: código descompilado tal cual, íconos, imágenes, textos de ayuda,
  nombre ni marca. El formulario y los recursos se redactan de nuevo.
- **Tablas normativas** (IEC 60364, CEI 64-8, NEC, CEC): las tomamos del APK para
  saber qué cubrir, pero cada valor se contrasta con la norma o una fuente pública
  antes de usarlo.
- **Fuera de alcance**: PairIP, chequeo de licencia, RevenueCat y la publicidad. Si
  queremos monetizar, se diseña aparte.

## Lo que ya sabemos

- 10.228 clases. 381 son de la app y 6.159 están ofuscadas en `defpackage` (ahí está
  la lógica de cálculo).
- La descompilación con JADX sale legible: el Fragment arma la UI y el botón de
  calcular delega en clases de `defpackage`.
- **Riesgo 1, PairIP**: hay unos 30 assets cifrados y la librería `libpairipcore.so`.
  PairIP puede virtualizar métodos (los saca del DEX y los ejecuta en su VM). Hay que
  detectar si algún cálculo quedó virtualizado.
- **Riesgo 2, código nativo**: `libandroidutils-native-lib.so` (1 MB) y
  `libf-native-lib.so` podrían contener lógica. Se revisan con las herramientas
  nativas de REA (Ghidra).
- Este entorno no tiene KVM, así que no hay emulador aquí. Para validar contra la app
  real se usa un teléfono o emulador local vía adb (opcional).

## Inventario funcional (≈ 330 pantallas)

| Sección | Pantallas | Ejemplos |
|---|---|---|
| Calculadoras (`main`) | 132 | Caída de tensión, sección IEC/CEI/NEC/CEC, cortocircuito, potencias, Ohm, códigos de colores, fusibles, puesta a tierra, compensación reactiva, canalizaciones, protecciones, LED, divisores, termistores |
| Motores (`motor`) | 21 | Corriente, potencia, rendimiento, deslizamiento, velocidad, par, condensador de arranque, conexiones y bornes IEC/NEMA |
| Conversiones (`conversions`) | 25 | AWG/SWG, Ah↔kWh, var↔µF, Hz↔rad/s, triángulo↔estrella, energía, potencia, presión, temperatura |
| Formulario (`formulario`) | 91 | Una ficha de fórmula por calculadora |
| Recursos (`resources`) | 44 | ANSI, IP, ATEX, enchufes del mundo, conectores EV, simbología, reactancia y peso de cables, valores E-series |
| Pinouts (`pinouts`) | 31 | USB, HDMI, RJ45/PoE, RS232, OBD2, Raspberry Pi, XLR, SATA… |
| Varios (`various`) | 24 | Splash, menú, favoritos, ajustes, exportar PDF, widget, guía, chatbot |

Varias pantallas son variantes por norma (`Base` + `Iec`/`Cei`/`Nec`/`Cec`): la
lógica común se extrae una sola vez y cada norma aporta sus tablas.

---

## Fase 1: Inventario y mapa (catálogo)

1. Decodificar recursos (layouts, `strings.xml` en es/en/it, menús, `res/raw`) con
   JADX, o con Apktool vía `decode_android_resources`.
2. Reconstruir el árbol de navegación desde `ActivityMain` y las listas de
   secciones: qué pantalla cuelga de cuál y si es gratuita o PRO.
3. Por cada pantalla registrar en `spec/catalogo.json`: id, sección, norma, entradas
   (con unidades y valores por defecto), salidas, layout asociado, clase y fórmula
   asociada.
4. Revisar PairIP: buscar métodos cuyo cuerpo llame a `com.pairip.VMRunner`, y
   llamadas `native` hacia las `.so`.

**Entregable**: `spec/catalogo.json` y un informe de riesgos con la lista de métodos
virtualizados o nativos.

## Fase 2: Extracción de lógica (el "estrujado")

Por cada calculadora, desde la pantalla hasta la fórmula:

1. Ubicar el listener del botón calcular (en `FragmentCalcoloCaduta` es
   `g4(this, 7)`) y seguirlo con `trace-android-references` e
   `inspect-android-method` hasta las clases de `defpackage`.
2. Renombrar mentalmente las clases ofuscadas (`pf0` = tipo de corriente, etc.) y
   anotarlo en `spec/simbolos.md` para no repetir trabajo.
3. Documentar en `spec/calc/<id>.md`: fórmula en notación matemática, entradas,
   rangos y validaciones, mensajes de error, redondeos, unidades y casos especiales
   (monofásica/trifásica, CC/CA).
4. Extraer las tablas a `data/<norma>/<tabla>.json`: ampacidad por tipo de
   instalación, factores de corrección por temperatura y agrupamiento, reactancias,
   resistividades, secciones normalizadas, calibres de protecciones, etc.
5. Si algo está virtualizado por PairIP o en código nativo, reconstruirlo desde la
   norma y el formulario de la app, y luego validarlo con vectores (Fase 3).

Orden por prioridad:

| Ola | Contenido |
|---|---|
| 1 | Ohm, potencias (P/Q/S), factor de potencia, corriente, tensión, resistencia, impedancia, reactancia |
| 2 | Caída de tensión (incluida la de cargas distribuidas), sección IEC/CEI/NEC/CEC (base y avanzada), ampacidad de conductores aislados y desnudos, longitud máxima, pérdidas, temperatura del cable |
| 3 | Cortocircuito (en transformador, aguas abajo, mínimo), protección del cable, energía específica pasante, dimensionamiento de protecciones, SPD, puesta a tierra, neutro, compensación reactiva |
| 4 | Motores (21) y transformador primario/secundario |
| 5 | Electrónica: códigos de colores y SMD, condensadores, inductores, LED, zener, divisores, resonancia, antenas, baterías, termistores, señales analógicas |
| 6 | Conversiones (25) |
| 7 | Recursos y pinouts: solo se extraen los datos; los textos se redactan de nuevo |

**Entregable**: `spec/calc/*.md` y `data/**/*.json` para todas las calculadoras.

## Fase 3: Vectores dorados (para no equivocarnos)

1. **Arnés JVM**: compilar en un proyecto de prueba aislado las clases descompiladas
   de cálculo puro (sin Android) y ejecutarlas con entradas de muestra para generar
   `tests/golden/<id>.json` con entradas y salidas esperadas. Este código no entra
   en nuestra app; solo sirve de oráculo.
2. Para lo que no se pueda ejecutar así, calcular los casos a mano desde la norma o
   con ejemplos resueltos publicados.
3. Opcional: comprobar puntualmente con la app instalada en un dispositivo vía adb
   (`start_adb_activity`, `capture_adb_screen`).

**Entregable**: vectores dorados por calculadora, con al menos 5 casos que incluyan
bordes y errores.

## Fase 4: Arquitectura de Electricalc

Propuesta, por confirmar: **Kotlin + Jetpack Compose**, con el motor de cálculo en
un módulo Kotlin puro.

```
electricalc/
├── core-calc/      # Kotlin puro: fórmulas, unidades, validación (sin Android) → testeable y portable a KMP
├── core-data/      # Tablas normativas (JSON) + repositorio
├── core-ui/        # Componentes Compose: campo con unidad, selector de norma, tabla de resultados
├── feature-*/      # Calculadoras, motores, conversiones, formulario, recursos, pinouts
└── app/            # Navegación, favoritos, historial, ajustes, exportar PDF, widget
```

- Una definición declarativa por calculadora (entradas, salidas y función) para
  generar la UI de forma uniforme y no escribir 330 pantallas a mano.
- Unidades con tipos (V, A, W, mm²/AWG, m/ft) y conversión automática.
- Norma por defecto según el país (IEC, NEC o CEC), configurable.
- Idiomas: español primero, luego inglés.
- Alternativa si se quiere iOS y web: Kotlin Multiplatform + Compose
  Multiplatform. `core-calc` sirve igual en ambos casos.

## Fase 5: Implementación por olas

Las mismas olas de la Fase 2. Para cada calculadora:

1. Pasar `core-calc` con tests contra los vectores dorados.
2. Escribir la definición de UI y el texto de ayuda propio.
3. Registrarla en el catálogo de navegación.

Una ola se da por terminada cuando todos sus tests dorados pasan.

## Fase 6: Verificación y cobertura

- Seguir la cobertura con REA (`build_reconstruction_obligation_ledger`,
  `evaluate_reconstruction_coverage`): cada pantalla del catálogo queda
  implementada, descartada a propósito o pendiente.
- CI con tests unitarios de `core-calc` y lint.
- Revisión final de que no quedó código, texto ni asset copiado.

## Fase 7: Extras propios (opcional)

Historial de cálculos, exportación de memorias de cálculo en PDF, normativa chilena
(NCh Elec. 4/2003 / pliegos técnicos SEC), modo offline completo y tema oscuro.

---

## Cómo lo ejecutamos

- Las Fases 1, 2 y 3 se pueden repartir entre varios agentes en paralelo (uno por
  ola), siempre que antes se cierre la Fase 1 y `spec/simbolos.md` esté
  compartido.
- Cada ola deja commits propios en `spec/`, `data/` y `tests/golden/`.
- La implementación (Fase 5) empieza apenas termine la Ola 1 de extracción; no hay
  que esperar al resto.

## Decisiones tomadas

1. **Stack**: Kotlin + Jetpack Compose, con `core-calc` en Kotlin puro.
2. **Normativa chilena**: se incluyen los Pliegos Técnicos Normativos RIC de la SEC
   (DS 8/2019) como norma seleccionable junto a IEC, NEC y CEC. Ver Fase 4b.
3. **Modelo**: app gratuita, sin anuncios ni compras. La arquitectura deja un punto
   de extensión (`FuncionPremium`) por si se monetiza más adelante, pero hoy todo está
   desbloqueado.

## Fase 4b: Normativa chilena (RIC)

- Norma `RIC` seleccionable y por defecto cuando el país es Chile.
- Pliegos relevantes para las calculadoras: RIC N°03 (alimentadores y demanda), N°04
  (conductores y canalizaciones: capacidad de corriente, factores de corrección,
  secciones mínimas), N°05 (protección contra tensiones peligrosas, diferenciales), N°06
  (puesta a tierra) y N°10 (instalaciones de uso general).
- Cada tabla y límite (p. ej. caídas de tensión máximas admisibles) se transcribe desde
  el texto oficial publicado por la SEC, con la referencia exacta (pliego, tabla,
  numeral) en `data/ric/`. No se usa ningún valor RIC sin su fuente.
- Calculadoras con verificación RIC: sección y caída de tensión (con semáforo de
  cumplimiento), protecciones, puesta a tierra, canalizaciones.

## Estado

- [x] Fase 0: REA, JADX y Apktool instalados; APK descompilado.
- [x] Riesgos revisados: PairIP solo virtualiza receptores de sistema y anuncios, y el
  código nativo solo hace verificación de licencia. Las fórmulas están en el DEX legible.
- [x] Fase 1 (parcial): menú completo extraído a `spec/catalogo.json` (155 herramientas
  en 5 secciones + 66 fichas de formulario, con marca PRO).
- [x] Proyecto Gradle con `core-calc` (Kotlin JVM) y tests JUnit 5.
- [x] Ola 1 (motor): `LeyOhm` cubre V, I, R, Z, P, Q, S y cosφ en CC, monofásica,
  bifásica y trifásica. Coincide con la app de referencia en 2.016 vectores dorados.
- [x] Ola 2 (parcial): `CaidaTension` y tablas de conductores (mm² y AWG), 1.500 vectores
  dorados coinciden.
- [x] **Oráculo directo**: el bytecode original corre en la JVM (dex2jar + stubs de Android
  + org.json, con `-noverify`). Los vectores dorados se generan llamando a las clases
  originales por reflexión, sin transcribir código. Ver `scripts/setup-oraculo.sh`.
- [x] Módulo `app` (Compose) con la calculadora de ley de Ohm. Se compila en CI (GitHub
  Actions genera el APK de debug como artefacto); en el entorno de nube no hay SDK de Android
  porque dl.google.com está bloqueado.
- [ ] Siguiente: dimensionamiento de conductores (IEC/CEI/NEC/CEC + RIC). Motor original en
  `cn2` (modelo de instalación y tablas de capacidad) y `zu` (algoritmo de selección).
- [ ] Olas 2 a 7.
