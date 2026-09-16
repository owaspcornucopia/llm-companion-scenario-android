param(
    [string]$AvdName = "Pixel_6_API_35",
    [int]$ApiLevel = 35,
    [string]$DeviceProfile = "pixel_6",
    [string]$SystemImage = "",
    [string]$SdkRoot = "",
    [switch]$SkipSdkSetup,
    [switch]$SkipBuild,
    [switch]$WipeData,
    [int]$MemoryMb = 16384,
    [int]$ProcessorCount = 8,
    [int]$ExpectedSwapMb = 12288
)

$ErrorActionPreference = "Stop"

if ($MemoryMb -le 0 -or $ProcessorCount -le 0 -or $ExpectedSwapMb -le 0) {
    throw "MemoryMb, ProcessorCount, and ExpectedSwapMb must be greater than zero."
}

if ([string]::IsNullOrWhiteSpace($SystemImage)) {
    $SystemImage = "system-images;android-$ApiLevel;google_apis;x86_64"
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

function Resolve-SdkTool {
    param(
        [string]$ToolName
    )

    $toolCandidates = @(
        (Join-Path $script:AndroidSdkRoot "cmdline-tools\latest\bin\$ToolName.bat"),
        (Join-Path $script:AndroidSdkRoot "tools\bin\$ToolName.bat")
    )
    $versionedTools = Join-Path $script:AndroidSdkRoot "cmdline-tools"
    if (Test-Path $versionedTools) {
        $toolCandidates += Get-ChildItem -Path $versionedTools -Directory |
                Where-Object { $_.Name -ne "latest" } |
                ForEach-Object { Join-Path $_.FullName "bin\$ToolName.bat" }
    }
    foreach ($candidate in $toolCandidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }
    throw "$ToolName was not found under $script:AndroidSdkRoot. Install Android command-line tools."
}

function Assert-Java {
    if ($env:JAVA_HOME) {
        $javaPath = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (-not (Test-Path $javaPath)) {
            throw "JAVA_HOME does not point to a JDK: $env:JAVA_HOME"
        }
    } elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw "Java 17 is required. Set JAVA_HOME to a JDK 17 installation or add java to PATH."
    }
}

function Get-SystemImageRelativePath {
    param(
        [string]$PackageName
    )

    return ($PackageName -replace "^system-images;", "system-images\" -replace ";", "\")
}

function Install-AndroidPackages {
    param(
        [object[]]$Packages
    )

    $missing = @(
        $Packages | Where-Object { -not (Test-Path (Join-Path $script:AndroidSdkRoot $_.Path)) }
    )
    if ($missing.Count -eq 0) {
        return
    }
    if ($SkipSdkSetup) {
        $names = ($missing | ForEach-Object { $_.Name }) -join ", "
        throw "Required Android SDK packages are missing: $names. Remove -SkipSdkSetup to install them."
    }

    $androidCli = Join-Path $script:AndroidSdkRoot "cmdline-tools\latest\bin\android.exe"
    if (Test-Path $androidCli) {
        Write-Host "Installing Android SDK packages with the Android CLI..."
        foreach ($package in $missing) {
            $packageName = $package.Name -replace ";", "/"
            $previousErrorActionPreference = $ErrorActionPreference
            try {
                $ErrorActionPreference = "Continue"
                $packageOutput = & $androidCli "--sdk=$script:AndroidSdkRoot" "sdk" "install" $packageName 2>&1
                $packageExitCode = $LASTEXITCODE
            } finally {
                $ErrorActionPreference = $previousErrorActionPreference
            }
            $packageOutput | Out-Host
            if ($packageExitCode -ne 0) {
                $packagePath = Join-Path $script:AndroidSdkRoot $package.Path
                if (-not (Test-Path $packagePath)) {
                    throw "Android SDK package installation failed for $($package.Name) with exit code $packageExitCode. Install it with Android Studio SDK Manager, then rerun with -SkipSdkSetup."
                }
                Write-Warning "Android CLI returned exit code $packageExitCode after installing $($package.Name); the package path exists, so continuing."
            }
        }
        foreach ($package in $missing) {
            if (-not (Test-Path (Join-Path $script:AndroidSdkRoot $package.Path))) {
                throw "Android SDK package was not installed: $($package.Name)"
            }
        }
        return
    }

    $sdkmanager = Resolve-SdkTool "sdkmanager"
    Write-Host "Accepting Android SDK licenses..."
    $licenseAnswers = 1..100 | ForEach-Object { "y" }
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $licenseOutput = $licenseAnswers | & $sdkmanager "--sdk_root=$script:AndroidSdkRoot" --licenses 2>&1
        $licenseExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $licenseOutput | Out-Host
    if ($licenseExitCode -ne 0) {
        throw "Android SDK license acceptance failed with exit code $licenseExitCode."
    }

    $packageNames = @($missing | ForEach-Object { $_.Name })
    Write-Host "Installing Android SDK packages: $($packageNames -join ', ')"
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $installOutput = & $sdkmanager "--sdk_root=$script:AndroidSdkRoot" @packageNames 2>&1
        $installExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $installOutput | Out-Host
    if ($installExitCode -ne 0) {
        throw "Android SDK package installation failed with exit code $installExitCode."
    }

    foreach ($package in $missing) {
        if (-not (Test-Path (Join-Path $script:AndroidSdkRoot $package.Path))) {
            throw "Android SDK package was not installed: $($package.Name)"
        }
    }
}

function Get-RunningEmulator {
    param(
        [string]$ExpectedAvdName
    )

    $deviceLines = & $script:AdbPath devices
    foreach ($line in $deviceLines) {
        if ($line -match "^(emulator-\d+)\s+device$") {
            $candidate = $matches[1]
            $runningAvdName = ((& $script:AdbPath -s $candidate emu avd name 2>$null |
                    Select-Object -First 1) -join "").Trim()
            if ($runningAvdName -eq $ExpectedAvdName) {
                return $candidate
            }
        }
    }
    return $null
}

function Resolve-AvdConfig {
    param(
        [string]$Name
    )

    $avdHome = if ($env:ANDROID_AVD_HOME) {
        $env:ANDROID_AVD_HOME
    } elseif ($env:ANDROID_EMULATOR_HOME) {
        Join-Path $env:ANDROID_EMULATOR_HOME "avd"
    } else {
        Join-Path $env:USERPROFILE ".android\avd"
    }
    $pointer = Join-Path $avdHome "$Name.ini"
    if (Test-Path $pointer) {
        $pathEntry = Get-Content $pointer | Where-Object { $_ -like "path=*" } | Select-Object -First 1
        if ($pathEntry) {
            $avdDirectory = $pathEntry.Substring("path=".Length)
            return Join-Path $avdDirectory "config.ini"
        }
    }
    return Join-Path $avdHome "$Name.avd\config.ini"
}

function Set-AvdProperty {
    param(
        [string[]]$Content,
        [string]$Name,
        [string]$Value
    )

    $replacement = "$Name=$Value"
    $found = $false
    $updated = @($Content | ForEach-Object {
        if ($_ -match "^$([regex]::Escape($Name))=") {
            $found = $true
            $replacement
        } else {
            $_
        }
    })
    if (-not $found) {
        $updated += $replacement
    }
    return $updated
}

function Get-GuestResources {
    param(
        [string]$Serial
    )

    $memoryInfo = & $script:AdbPath -s $Serial shell cat /proc/meminfo
    $memoryKb = [long](([regex]::Match(($memoryInfo -join "`n"), "(?m)^MemTotal:\s+(\d+)")).Groups[1].Value)
    $swapKb = [long](([regex]::Match(($memoryInfo -join "`n"), "(?m)^SwapTotal:\s+(\d+)")).Groups[1].Value)
    $cores = [int](((& $script:AdbPath -s $Serial shell getconf _NPROCESSORS_ONLN) -join "").Trim())
    return [pscustomobject]@{
        MemoryMb = [math]::Floor($memoryKb / 1024)
        ProcessorCount = $cores
        SwapMb = [math]::Floor($swapKb / 1024)
    }
}

Assert-Java
$script:AndroidSdkRoot = Resolve-SdkRoot
$env:ANDROID_SDK_ROOT = $script:AndroidSdkRoot
$env:ANDROID_HOME = $script:AndroidSdkRoot
$env:Path = "$(Join-Path $script:AndroidSdkRoot 'platform-tools');$(Join-Path $script:AndroidSdkRoot 'emulator');$(Join-Path $script:AndroidSdkRoot 'cmdline-tools\latest\bin');$env:Path"

$script:AdbPath = Join-Path $script:AndroidSdkRoot "platform-tools\adb.exe"
$emulatorPath = Join-Path $script:AndroidSdkRoot "emulator\emulator.exe"
if (-not (Test-Path $script:AdbPath) -or -not (Test-Path $emulatorPath)) {
    $sdkmanagerPackages = @(
        [pscustomobject]@{ Name = "platform-tools"; Path = "platform-tools\adb.exe" },
        [pscustomobject]@{ Name = "emulator"; Path = "emulator\emulator.exe" }
    )
    Install-AndroidPackages $sdkmanagerPackages
}

$requiredPackages = @(
    [pscustomobject]@{ Name = "platform-tools"; Path = "platform-tools\adb.exe" },
    [pscustomobject]@{ Name = "emulator"; Path = "emulator\emulator.exe" },
    [pscustomobject]@{ Name = "platforms;android-$ApiLevel"; Path = "platforms\android-$ApiLevel" },
    [pscustomobject]@{ Name = "build-tools;35.0.0"; Path = "build-tools\35.0.0" },
    [pscustomobject]@{ Name = "ndk;26.3.11579264"; Path = "ndk\26.3.11579264" },
    [pscustomobject]@{ Name = "cmake;3.31.6"; Path = "cmake\3.31.6" },
    [pscustomobject]@{
        Name = $SystemImage
        Path = Get-SystemImageRelativePath $SystemImage
    }
)
Install-AndroidPackages $requiredPackages

$avdmanager = Resolve-SdkTool "avdmanager"
$existingAvds = @(& $avdmanager list avd -c 2>$null |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ })
if ($existingAvds -notcontains $AvdName) {
    Write-Host "Creating AVD $AvdName with $SystemImage..."
    $createOutput = "no" | & $avdmanager create avd `
        --force `
        --name $AvdName `
        --package $SystemImage `
        --device $DeviceProfile 2>&1
    $createExitCode = $LASTEXITCODE
    $createOutput | Out-Host
    $existingAvds = @(& $avdmanager list avd -c 2>$null |
            ForEach-Object { $_.Trim() } |
            Where-Object { $_ })
    if ($existingAvds -notcontains $AvdName) {
        throw "AVD creation failed with exit code $createExitCode."
    }
    if ($createExitCode -ne 0) {
        Write-Warning "avdmanager returned exit code $createExitCode, but $AvdName was created; continuing."
    }
}

$avdConfig = Resolve-AvdConfig $AvdName
if (-not (Test-Path $avdConfig)) {
    throw "AVD configuration was not found: $avdConfig"
}
$avdContent = Get-Content $avdConfig
$avdContent = Set-AvdProperty $avdContent "hw.ramSize" "${MemoryMb}M"
$avdContent = Set-AvdProperty $avdContent "hw.cpu.ncore" "$ProcessorCount"
Set-Content -Path $avdConfig -Value $avdContent

if (-not $SkipBuild) {
    & (Join-Path $PSScriptRoot "build-embedded-apk.ps1")
    if ($LASTEXITCODE -ne 0) {
        throw "Debug APK build failed with exit code $LASTEXITCODE."
    }
}

$apk = Join-Path $PSScriptRoot "..\app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) {
    throw "APK is missing: $apk"
}

$adbStartOutput = & $script:AdbPath start-server 2>&1
$adbStartOutput | Out-Host
$serial = Get-RunningEmulator $AvdName
if ($serial) {
    $runningResources = Get-GuestResources $serial
    if ($runningResources.MemoryMb -lt ($MemoryMb * 0.9)) {
        Write-Host "Restarting $AvdName to apply $MemoryMb MB RAM and $ProcessorCount processors..."
        & $script:AdbPath -s $serial emu kill | Out-Host
        do {
            Start-Sleep -Seconds 2
            $serial = Get-RunningEmulator $AvdName
        } while ($serial)
    }
}
if (-not $serial) {
    Write-Host "Starting AVD $AvdName..."
    $emulatorArguments = @(
        "-avd", $AvdName,
        "-memory", "$MemoryMb",
        "-cores", "$ProcessorCount",
        "-no-snapshot",
        "-no-boot-anim"
    )
    if ($WipeData) {
        $emulatorArguments += "-wipe-data"
    }
    Start-Process -FilePath $emulatorPath -ArgumentList $emulatorArguments
}

Write-Host "Waiting for AVD $AvdName to finish booting; no timeout is applied..."
do {
    Start-Sleep -Seconds 3
    $serial = Get-RunningEmulator $AvdName
    if ($serial) {
        $bootCompleted = ((& $script:AdbPath -s $serial shell getprop sys.boot_completed 2>$null) -join "").Trim()
        if ($bootCompleted -eq "1") {
            break
        }
    }
} while ($true)

$resources = Get-GuestResources $serial
if ($resources.MemoryMb -lt ($MemoryMb * 0.9)) {
    throw "The emulator started with $($resources.MemoryMb) MB RAM instead of the requested $MemoryMb MB."
}
if ($resources.ProcessorCount -lt $ProcessorCount) {
    Write-Warning "The emulator exposed $($resources.ProcessorCount) processors after requesting $ProcessorCount; Android Emulator may cap the guest CPU count."
}
if ($resources.SwapMb -lt ($ExpectedSwapMb * 0.9)) {
    Write-Warning "Android provisioned $($resources.SwapMb) MB of compressed swap; the image controls zram and did not reach the requested equivalent of $ExpectedSwapMb MB."
}
Write-Host "Emulator resources: $($resources.MemoryMb) MB RAM, $($resources.ProcessorCount) processors, $($resources.SwapMb) MB compressed swap."

$installOutput = & $script:AdbPath -s $serial install -r $apk 2>&1
$installExitCode = $LASTEXITCODE
$installOutput | Out-Host
if ($installExitCode -ne 0) {
    throw "APK installation failed with exit code $installExitCode."
}
& $script:AdbPath -s $serial shell am start -n "org.owasp.pwnednext.android/.MainActivity" | Out-Host
Write-Host "AI Anti Fraud 3.0 is running on $serial."
