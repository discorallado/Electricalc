#!/usr/bin/env bash
# Instala REA (https://github.com/morluto/rea) y JADX headless para analizar el APK.
# Requisitos: Node.js 22+, JDK 17+ (con javac) y `ps` (procps).
set -euo pipefail

REA_VERSION="${REA_VERSION:-6.7.0}"
JADX_DIR="${HOME}/.local/share/rea/android"
JADX_JAR="${JADX_DIR}/jadx-headless-mcp-0.7.1-all.jar"
JADX_SHA256="6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75"

npm install --global "rea-agents@${REA_VERSION}"

mkdir -p "${JADX_DIR}"
if [ ! -f "${JADX_JAR}" ]; then
  curl -fL -o "${JADX_JAR}" \
    https://github.com/1013503897/jadx-headless-mcp/releases/download/v0.7.1/jadx-headless-mcp-0.7.1-all.jar
fi
echo "${JADX_SHA256}  ${JADX_JAR}" | sha256sum -c -

# Extrae el APK base desde el XAPK si se entrega como argumento.
if [ "${1:-}" != "" ]; then
  mkdir -p apk
  unzip -oq "$1" -d apk
  echo "APK base: $(pwd)/apk/it.Ettore.calcolielettrici.apk"
fi

echo "Listo. Exporta antes de usar rea:"
echo "  export REA_JADX_MCP_JAR=${JADX_JAR}"
