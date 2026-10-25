# Instala REA (https://github.com/morluto/rea) y JADX headless para analizar el APK.
# Requisitos: Node.js 22+ y Android Studio (o un JDK 17+).
# Uso: .\scripts\setup-rea.ps1
param([string]$ReaVersion = "6.7.0")
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

npm install --global "rea-agents@$ReaVersion"
if ($LASTEXITCODE -ne 0) { throw "npm install fallo (codigo $LASTEXITCODE)" }

$dir = "$HOME\.local\share\rea\android"
$jar = "$dir\jadx-headless-mcp-0.7.1-all.jar"
$sha = "6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75"
New-Item -ItemType Directory -Force $dir | Out-Null
if (-not (Test-Path $jar)) {
    Invoke-WebRequest -Uri "https://github.com/1013503897/jadx-headless-mcp/releases/download/v0.7.1/jadx-headless-mcp-0.7.1-all.jar" -OutFile $jar
}
if ((Get-FileHash -Algorithm SHA256 $jar).Hash.ToLowerInvariant() -ne $sha) {
    Remove-Item $jar
    throw "El SHA-256 del JAR de JADX no coincide con el publicado por REA"
}

# Variable de usuario persistente: la leen REA y Claude Code en nuevas terminales.
[Environment]::SetEnvironmentVariable("REA_JADX_MCP_JAR", $jar, "User")
$env:REA_JADX_MCP_JAR = $jar
Write-Host "Listo. REA_JADX_MCP_JAR=$jar (abre una terminal nueva para que la tomen otros programas)"