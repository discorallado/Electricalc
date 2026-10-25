# Prepara las herramientas para extraer la app de referencia y ejecutarla como oraculo.
# Uso: .\scripts\setup-oraculo.ps1 -Xapk C:\ruta\archivo.xapk [-Trabajo C:\otra\carpeta]
# Todo queda fuera del repositorio (por defecto en %USERPROFILE%\.cache\electricalc-oraculo).
param(
    [Parameter(Mandatory = $true)][string]$Xapk,
    [string]$Trabajo = "$HOME\.cache\electricalc-oraculo"
)
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"   # acelera Invoke-WebRequest
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Buscar-Java {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) { return "$env:JAVA_HOME\bin\java.exe" }
    $studio = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
    if (Test-Path $studio) { return $studio }
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw "No se encontro Java. Instala Android Studio o define JAVA_HOME."
}
function Descargar($url, $destino) {
    if (-not (Test-Path $destino)) { Invoke-WebRequest -Uri $url -OutFile $destino }
}
function Extraer($zip, $destino) {
    if (Test-Path $destino) { Remove-Item -Recurse -Force $destino }
    [IO.Compression.ZipFile]::ExtractToDirectory($zip, $destino)
}

$java = Buscar-Java
$env:JAVA_HOME = Split-Path (Split-Path $java)   # jadx.bat y d2j-dex2jar.bat usan JAVA_HOME
$herr = Join-Path $Trabajo "herramientas"
$jar  = Join-Path $Trabajo "jar"
New-Item -ItemType Directory -Force $herr, $jar | Out-Null

Descargar "https://github.com/skylot/jadx/releases/download/v1.5.3/jadx-1.5.3.zip" "$herr\jadx.zip"
Descargar "https://github.com/iBotPeaches/Apktool/releases/download/v2.12.1/apktool_2.12.1.jar" "$herr\apktool.jar"
Descargar "https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip" "$herr\d2j.zip"
Descargar "https://repo1.maven.org/maven2/com/google/android/android/4.1.1.4/android-4.1.1.4.jar" "$jar\android-stub.jar"
Descargar "https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar" "$jar\json.jar"
if (-not (Test-Path "$herr\jadx")) { Extraer "$herr\jadx.zip" "$herr\jadx" }
if (-not (Test-Path "$herr\d2j"))  { Extraer "$herr\d2j.zip"  "$herr\d2j" }

Extraer $Xapk (Join-Path $Trabajo "xapk")
$apk = Join-Path $Trabajo "xapk\it.Ettore.calcolielettrici.apk"

# Codigo Java legible (sin reemplazar constantes y mostrando metodos que JADX no reconstruye bien).
# JADX termina con algunos errores (~60 metodos); es normal.
& "$herr\jadx\bin\jadx.bat" --no-res --no-replace-consts --show-bad-code -j 4 -d "$Trabajo\src" $apk
# Recursos decodificados (strings, layouts).
& $java -jar "$herr\apktool.jar" d --no-src -f -o "$Trabajo\res" $apk
# Bytecode original como JAR, para ejecutarlo en la JVM.
& "$herr\d2j\dex-tools-v2.4\d2j-dex2jar.bat" -f -o "$jar\app.jar" $apk

Write-Host "Listo en $Trabajo"
Write-Host "Classpath del oraculo: $jar\app.jar;$jar\android-stub.jar;$jar\json.jar (java -noverify)"