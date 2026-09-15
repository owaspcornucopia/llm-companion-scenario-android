param(
    [switch]$SkipModelDownload
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$modelPath = Join-Path $repoRoot "models\tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"
$gradle = Join-Path $repoRoot "gradlew.bat"

if ($env:JAVA_HOME) {
    $javaPath = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (-not (Test-Path $javaPath)) {
        throw "JAVA_HOME does not point to a JDK: $env:JAVA_HOME"
    }
} elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java 17 is required. Set JAVA_HOME to a JDK 17 installation or add java to PATH."
}

if (-not (Test-Path $modelPath)) {
    if ($SkipModelDownload) {
        throw "Embedded model is missing: $modelPath"
    }
    & (Join-Path $PSScriptRoot "download-model.ps1")
    if ($LASTEXITCODE -ne 0) {
        throw "Model download failed with exit code $LASTEXITCODE."
    }
}

& $gradle ":app:assembleDebug"
if ($LASTEXITCODE -ne 0) {
    throw "Embedded APK build failed with exit code $LASTEXITCODE."
}

$apk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) {
    throw "Embedded APK is missing: $apk"
}

Write-Host "Embedded-model APK is ready: $apk"
