#!/usr/bin/env bash
# Prepara las herramientas para extraer la app de referencia y ejecutarla como oráculo.
# Uso: ./scripts/setup-oraculo.sh /ruta/al/archivo.xapk [directorio-de-trabajo]
# Todo queda fuera del repositorio (por defecto en ~/.cache/electricalc-oraculo).
set -euo pipefail

XAPK="${1:?Indica la ruta del .xapk}"
TRABAJO="${2:-$HOME/.cache/electricalc-oraculo}"
HERR="$TRABAJO/herramientas"
mkdir -p "$HERR" "$TRABAJO/xapk" "$TRABAJO/jar"

descargar() { [ -f "$2" ] || curl -fL --retry 3 -o "$2" "$1"; }

descargar https://github.com/skylot/jadx/releases/download/v1.5.3/jadx-1.5.3.zip "$HERR/jadx.zip"
descargar https://github.com/iBotPeaches/Apktool/releases/download/v2.12.1/apktool_2.12.1.jar "$HERR/apktool.jar"
descargar https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip "$HERR/d2j.zip"
descargar https://repo1.maven.org/maven2/com/google/android/android/4.1.1.4/android-4.1.1.4.jar "$TRABAJO/jar/android-stub.jar"
descargar https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar "$TRABAJO/jar/json.jar"
[ -d "$HERR/jadx" ] || unzip -q "$HERR/jadx.zip" -d "$HERR/jadx"
[ -d "$HERR/d2j" ] || unzip -q "$HERR/d2j.zip" -d "$HERR/d2j"

unzip -oq "$XAPK" -d "$TRABAJO/xapk"
APK="$TRABAJO/xapk/it.Ettore.calcolielettrici.apk"

# Código Java legible (sin reemplazar constantes y mostrando métodos que JADX no reconstruye bien).
"$HERR/jadx/bin/jadx" --no-res --no-replace-consts --show-bad-code -j 4 -d "$TRABAJO/src" "$APK" || true
# Recursos decodificados (strings, layouts).
java -jar "$HERR/apktool.jar" d --no-src -f -o "$TRABAJO/res" "$APK"
# Bytecode original como JAR, para ejecutarlo en la JVM.
sh "$HERR/d2j/dex-tools-v2.4/d2j-dex2jar.sh" -f -o "$TRABAJO/jar/app.jar" "$APK"

echo "Listo en $TRABAJO"
echo "Classpath del oráculo: $TRABAJO/jar/app.jar:$TRABAJO/jar/android-stub.jar:$TRABAJO/jar/json.jar (java -noverify)"
