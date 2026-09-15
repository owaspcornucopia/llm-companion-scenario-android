param(
    [string]$ModelDirectory = (Join-Path $PSScriptRoot "..\models")
)

$ErrorActionPreference = "Stop"

# This quantized model avoids making every tester download a data center.
$baseRepository = "TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF"
$baseFile = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"
$adapterRepository = "Rj18/text-to-sql-tinyllama-lora"
$adapterFiles = @(
    "adapter_config.json",
    "adapter_model.safetensors",
    "tokenizer.json",
    "tokenizer_config.json"
)

New-Item -ItemType Directory -Force -Path $ModelDirectory | Out-Null

function Download-HuggingFaceFile {
    param(
        [string]$Repository,
        [string]$File,
        [string]$Destination
    )

    if (Test-Path $Destination) {
        Write-Host "Already present: $Destination"
        return
    }

    $encodedFile = [Uri]::EscapeDataString($File)
    $uri = "https://huggingface.co/$Repository/resolve/main/${encodedFile}?download=true"
    Write-Host "Downloading $Repository/$File"
    Invoke-WebRequest -Uri $uri -OutFile $Destination
}

Download-HuggingFaceFile `
    -Repository $baseRepository `
    -File $baseFile `
    -Destination (Join-Path $ModelDirectory $baseFile)

$adapterDirectory = Join-Path $ModelDirectory "text-to-sql-tinyllama-lora"
New-Item -ItemType Directory -Force -Path $adapterDirectory | Out-Null
foreach ($adapterFile in $adapterFiles) {
    Download-HuggingFaceFile `
        -Repository $adapterRepository `
        -File $adapterFile `
        -Destination (Join-Path $adapterDirectory $adapterFile)
}

Write-Host ""
Write-Host "Model files are ready in $ModelDirectory"
Write-Host "The Android app packages the GGUF and runs it inside the app through llama.cpp."
Write-Host "There is no host model service, heuristic provider, or inference fallback."
