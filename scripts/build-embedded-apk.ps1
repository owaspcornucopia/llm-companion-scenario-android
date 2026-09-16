param(
    [switch]$SkipModelDownload,
    [string]$SdkRoot = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$modelPath = Join-Path $repoRoot "models\tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"
$llamaSourcePath = Join-Path $repoRoot "third_party\llama.cpp"
$gradle = Join-Path $repoRoot "gradlew.bat"

function Ensure-LlamaSubmodule {
    if (Test-Path (Join-Path $llamaSourcePath "CMakeLists.txt")) {
        return
    }

    $git = Get-Command git -ErrorAction SilentlyContinue
    if (-not $git) {
        throw "llama.cpp is missing and git is not available. Install Git or clone third_party\llama.cpp before building."
    }

    Write-Host "Initializing llama.cpp source..."
    $updateOutput = & $git -C $repoRoot submodule update --init --recursive -- "third_party/llama.cpp" 2>&1
    $updateExitCode = $LASTEXITCODE
    if ($updateExitCode -ne 0) {
        $updateOutput | Out-Host
        Write-Warning "Git submodule initialization returned exit code $updateExitCode; trying the URL in .gitmodules."
    }
    if (Test-Path (Join-Path $llamaSourcePath "CMakeLists.txt")) {
        return
    }

    $submoduleUrl = (& $git -C $repoRoot config -f .gitmodules --get "submodule.third_party/llama.cpp.url" 2>$null |
            Select-Object -First 1).Trim()
    if ([string]::IsNullOrWhiteSpace($submoduleUrl)) {
        throw "llama.cpp is missing and no URL was found in .gitmodules."
    }

    New-Item -ItemType Directory -Force -Path (Split-Path $llamaSourcePath -Parent) | Out-Null
    $cloneOutput = & $git clone --depth 1 $submoduleUrl $llamaSourcePath 2>&1
    $cloneExitCode = $LASTEXITCODE
    if ($cloneExitCode -ne 0) {
        $cloneOutput | Out-Host
        throw "llama.cpp download failed with exit code $cloneExitCode."
    }
    if (-not (Test-Path (Join-Path $llamaSourcePath "CMakeLists.txt"))) {
        throw "llama.cpp download completed but its CMakeLists.txt was not found at $llamaSourcePath."
    }
}

function Resolve-SdkRoot {
    $candidates = @(
        $SdkRoot,
        $env:ANDROID_SDK_ROOT,
        $env:ANDROID_HOME,
        (Join-Path $env:LOCALAPPDATA "Android\Sdk"),
        (Join-Path $env:USERPROFILE "scoop\apps\android-clt\current")
    )
    foreach ($candidate in $candidates) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path $candidate)) {
            return (Resolve-Path $candidate).Path
        }
    }
    throw "Android SDK was not found. Set ANDROID_SDK_ROOT or pass -SdkRoot."
}

$androidSdkRoot = Resolve-SdkRoot
$env:ANDROID_SDK_ROOT = $androidSdkRoot
$env:ANDROID_HOME = $androidSdkRoot

$requiredSdkPaths = @(
    "platforms\android-35",
    "build-tools\35.0.0",
    "ndk\26.3.11579264",
    "cmake\3.31.6"
)
$missingSdkPaths = @(
    $requiredSdkPaths | Where-Object { -not (Test-Path (Join-Path $androidSdkRoot $_)) }
)
if ($missingSdkPaths.Count -gt 0) {
    throw "Required Android SDK packages are missing under $androidSdkRoot`: $($missingSdkPaths -join ', '). Install them with sdkmanager or run scripts/start-emulator.ps1."
}

Ensure-LlamaSubmodule

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
