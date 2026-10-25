# Traspaso a desarrollo local

Estado al cierre del trabajo en la nube y cómo continuar en tu máquina con Android Studio
y Claude Code.

## 1. Preparar el entorno local (Windows)

1. Clonar la rama de trabajo:
   ```powershell
   git clone -b ccr-a5593e17-q4r0gz https://github.com/discorallado/Electricalc.git
   cd Electricalc
   ```
2. Abrir la carpeta en **Android Studio Narwhal 3 (2025.1.3) o más nuevo**. Instalar el SDK 36
   si lo pide y sincronizar. Si el módulo `app` no aparece en el primer sync, sincronizar de
   nuevo: se incluye solo cuando existe `local.properties` con `sdk.dir`.
3. Verificar el motor y la app:
   ```powershell
   .\gradlew.bat :core-calc:test :app:assembleDebug
   ```
4. Copiar el `.xapk` de la app de referencia **fuera del repo** (está en `.gitignore`) y
   preparar el oráculo:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\setup-oraculo.ps1 -Xapk C:\ruta\electrical-calculations-11-2-1.xapk
   ```
   Deja en `%USERPROFILE%\.cache\electricalc-oraculo\`: código descompilado (`src\`), recursos
   decodificados (`res\`) y el bytecode original como JAR (`jar\app.jar`). En Windows el
   classpath de Java se separa con `;`.
5. Instalar REA y JADX headless para Claude Code:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\setup-rea.ps1
   ```
   `.mcp.json` registra REA como servidor MCP (`cmd /c npx -y rea-agents@6.7.0 mcp`).
6. Plugins recomendados para Claude Code (directorio de Anthropic):
   - **Android CLI**: compilar, instalar y ejecutar en emulador o dispositivo.
   - **KotlinSense** o **kotlin-lsp**: diagnósticos de tipos e imports en Kotlin.
   - **Android Emulator QA**: capturas, árbol de UI y logcat para validar pantallas.
   - **Design** (Anthropic): revisión de diseño, UX copy y accesibilidad.

Commits y PR se hacen desde la consola local.

## 2. Qué hay hecho

| Pieza | Estado |
|---|---|
| `spec/catalogo.json` | Menú completo del APK: 6 pestañas, 155 herramientas, 66 fichas de formulario |
| `core-calc` · `LeyOhm` | V, I, R, Z, P, Q, S, cosφ; CC/mono/bi/trifásica; 2.016 vectores dorados OK |
| `core-calc` · `CaidaTension` + tablas de conductores | mm² y AWG, Cu/Al, temperatura, cargas distribuidas; 1.500 vectores OK |
| `app` | UI según la sección 3: 8 calculadoras de ley de Ohm y caída de tensión, fichas, guías, favoritos, PDF. `CalculadorasTest` cubre todas las combinaciones |
| CI | GitHub Actions corre los tests y genera el APK de debug |
| Oráculo | El bytecode original corre en la JVM (`java -noverify -cp app.jar:android-stub.jar:json.jar`) |

## 3. Rediseño de la UI: replicar el esquema del APK

La pantalla de ley de Ohm actual (un selector de "qué calcular" más campos sueltos) **se
descarta**. La app debe seguir la estructura y la forma de explicar del APK.

### 3.1 Navegación (igual que el APK)

- Barra superior con menú lateral (drawer: ajustes, acerca de, guía) y **pestañas
  superiores con ícono**: **Principal, Motor, Conversiones, Recursos, Patillaje, Formulario,
  Favoritos**. El contenido y el orden salen de `spec/catalogo.json` (61, 17, 20, 27, 30 y
  66 ítems).
- Cada pestaña es una lista de filas con ícono y título. Al tocar una fila se abre su
  pantalla. Mantener pulsado agrega o quita de Favoritos.
- **Una pantalla por calculadora**, como en el APK: "Cálculo de la corriente", "Cálculo de la
  tensión", "Cálculo de la potencia activa", etc., cada una con su propio formulario. No una
  pantalla genérica con un selector de salida.
- Todo el contenido es gratuito: se ignora la marca PRO del catálogo.

### 3.2 Anatomía de una pantalla de cálculo

Referencia: `fragment_calcolo_caduta.xml`, `fragment_form_caduta.xml` y
`guida_parametro.xml` del APK.

1. **Barra superior** con el título y tres acciones: **Formula** (abre la ficha del
   formulario asociada, campo `formula` del catálogo), **Guía** (ayuda de la pantalla) y
   **Favorito**.
2. **Tipo de corriente** como control segmentado arriba (`TipoCorrenteView`: CC /
   Monofásica / Bifásica / Trifásica). Cambiarlo ajusta los campos visibles; por ejemplo, en
   CC se oculta el cosφ.
3. **Variantes de entrada**. Cuando una magnitud se puede calcular con distintos pares de
   datos (la corriente sale de V+P, V+R, V+Z, P+R o P+Z), se elige con un selector
   "Datos conocidos" que muestra solo esos campos. Las combinaciones salen de los `id1` /
   `gd1` del APK (ver `FragmentCalcoloIntensita.N0()`).
4. **Filas de entrada en tabla**: etiqueta, campo numérico con *hint* y **selector de unidad
   al lado** (V/kV, W/kW/HP, m/ft, mm²/AWG, °C/°F). La carga se ingresa como A, W, VA o var
   con conversión automática.
5. **Ícono ⓘ por campo**, que abre su explicación (textos `guida_*`; p. ej. tensión: fase-neutro
   en monofásica, fase-fase en bi y trifásica; cosφ: valores típicos por tipo de carga).
6. **Botón Calcular** y una **tarjeta de resultados** animada debajo, con la etiqueta, el
   valor y su unidad, y valores secundarios cuando aplica (p. ej. caída en V y en %, y tensión
   en la carga).
7. **Exportar a PDF** y compartir el cálculo (el APK lo hace en `b0()`).

### 3.3 Ficha de formulario

Una tarjeta por variante (CC, monofásica/bifásica, trifásica) con la fórmula renderizada
(fracciones, raíces, subíndices) y una **leyenda** de símbolos. Se accede desde la acción
Formula de cada calculadora y desde la pestaña Formulario.

### 3.4 Pantalla de guía

Una lista de tarjetas "parámetro → explicación" (`guida_parametro.xml`), más un párrafo
introductorio sobre qué calcula la pantalla. Es la estructura `ak0` / `pj2` de cada fragment
(`s0()`): `xj0.b` es la intro y cada `pj2` es un parámetro con sus textos.

### 3.5 Textos

Los textos del APK (`guida_*`, etiquetas) son la **referencia de contenido**: qué se explica y
con qué nivel de detalle. Se redactan de nuevo con palabras propias en `strings.xml` (es por
defecto, en inglés), y se puede agregar contexto chileno (RIC, SEC). No se copian
literalmente.

### 3.6 Arquitectura sugerida

```
app/ui/
  navegacion/   PantallaPrincipal (pestañas + drawer), Rutas generadas desde el catálogo
  catalogo/     Modelo de menú (sección → ítems) cargado desde un JSON de assets propio
  componentes/  FilaEntrada(etiqueta, hint, unidades, ayuda), SelectorTipoCorriente,
                SelectorVariante, TarjetaResultado, VistaFormula, DialogoAyuda
  calculadoras/ Una definición por pantalla: campos, variantes, cálculo (llama a core-calc),
                resultados, guía y fórmula asociada
  formulario/   Fichas con fórmulas renderizadas
```

Cada calculadora es una **definición declarativa** que se renderiza con los mismos
componentes. Así cada pantalla queda propia (título, campos, variantes, ayuda), pero sin
duplicar código.

## 4. Siguientes pasos, en orden

1. ~~Rediseño de la UI (sección 3)~~ Hecho. Para agregar una calculadora: una definición en
   `app/.../ui/calculadoras/` registrada en `Registro.kt`, su ficha en `Fichas.kt` y sus
   textos en `strings.xml` (es y en). Pendientes rápidos: longitud máxima del cable (ΔV) y
   caída con cargas distribuidas, que ya están en el motor.
2. Dimensionamiento de conductores IEC/CEI/NEC/CEC + RIC: motor original en `cn2`
   (instalación y tablas de capacidad, ver `in2` y `jn2`) y `zu` (algoritmo de selección).
   Validar con el oráculo, llamando a esas clases por reflexión.
3. Olas 3 a 7 según `docs/plan.md`.

## 5. Prompt para la sesión local de Claude Code

> Lee `docs/HANDOFF.md` y `docs/plan.md`. Rehaz el módulo `app` siguiendo la sección 3 del
> handoff: pestañas iguales al APK desde `spec/catalogo.json`, una pantalla por calculadora,
> con tipo de corriente, variantes de datos conocidos, filas con hint y unidad, ayuda ⓘ por
> campo, guía, ficha de fórmula y tarjeta de resultados. Empieza por las 8 calculadoras de la
> ley de Ohm y la de caída de tensión. Para contenido y comportamiento consulta el APK
> descompilado en `%USERPROFILE%\.cache\electricalc-oraculo\` (o usa REA). Compila y prueba en el emulador
> antes de darlo por terminado. No hagas commits; yo los hago.
