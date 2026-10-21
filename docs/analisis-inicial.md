# Análisis inicial: Electrical Calculations 11.2.1

Herramienta: [REA](https://github.com/morluto/rea) 6.7.0 + JADX headless 0.7.1 (JDK 21).

## Paquete

| Campo | Valor |
|---|---|
| Paquete | `it.Ettore.calcolielettrici` |
| Versión | 11.2.1 (code 492) |
| minSdk / targetSdk | 32 / 36 |
| SHA-256 APK base | `0b61c5764f9bedecfc004eb5bdcc7cf81b22d53d63b00a340d1e17a9c612ea14` |
| Clases totales | 10.228 (6.159 ofuscadas en `defpackage`) |
| Recursos | 1.731 |

El XAPK trae el APK base más splits de ABI (arm64-v8a, armeabi-v7a), densidad
(xhdpi, xxhdpi, xxxhdpi) e idiomas (ar, de, en, es, fr, in, it, iw, kn, ms, pt, ro, tr).

Dependencias principales: RevenueCat + Google Play Billing (compras), Firebase
(mensajería/analytics), PairIP (verificación de licencia), y librerías propias del
autor (`it.ettoregallina.*`: androidutils, expressions, materialpreferences, butils).

## Estructura de la app (`it.Ettore.calcolielettrici.ui`)

| Paquete | Clases | Contenido |
|---|---|---|
| `pages.main` | 132 | Calculadoras (Fragment por cálculo) |
| `pages.formulario` | 91 | Formulario / fórmulas |
| `pages.resources` | 44 | Tablas de referencia |
| `pages.pinouts` | 31 | Pinouts de conectores |
| `pages.conversions` | 25 | Conversiones de unidades |
| `pages.various` | 24 | Splash, Main, Ajustes, Billing, Guía, Chatbot, etc. |
| `pages.motor` | 21 | Cálculos de motores |
| `views` | 10 | Spinners personalizados (conductor, temperatura, tipo de corriente, instalación) |

Las calculadoras de `pages.main` cubren, entre otras: caída de tensión, sección
de conductor (normas IEC, CEI, NEC, CEC), corriente de cortocircuito, factor de
potencia, potencia activa/aparente/reactiva, ley de Ohm, código de colores de
resistencias/inductores/condensadores, fusibles, puesta a tierra, corrección del
factor de potencia, dimensionamiento de canalizaciones y protecciones, LED,
divisores de tensión/corriente, termistores (NTC, PT100, termocuplas).

La lista completa de clases está en [`clases-apk.txt`](clases-apk.txt).

## Observaciones

- Los nombres de los Fragment se conservan, pero los métodos están ofuscados por
  R8 (p. ej. `FragmentCalcoloCaduta.k0()`), y la lógica de cálculo vive en clases
  de `defpackage` (p. ej. `ak0`, `pf0`). Hay que seguir referencias desde cada
  Fragment para llegar a las fórmulas.
- La firma del APK no fue verificada (REA reporta `not_performed`).

## Comandos útiles

```sh
./scripts/setup-rea.sh /ruta/al/archivo.xapk
export REA_JADX_MCP_JAR=$HOME/.local/share/rea/android/jadx-headless-mcp-0.7.1-all.jar
APK=apk/it.Ettore.calcolielettrici.apk
rea inspect-android-package $APK
rea search-android-classes $APK FragmentCalcolo
rea inspect-android-class $APK it.Ettore.calcolielettrici.ui.pages.main.FragmentCalcoloCaduta
rea inspect-android-method $APK it.Ettore.calcolielettrici.ui.pages.main.FragmentCalcoloCaduta s0
rea trace-android-references $APK ak0
```
