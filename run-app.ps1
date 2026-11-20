<#
.SYNOPSIS
    Build the app, pick devices (connected ones or emulators to boot), install it on them and launch it.

.EXAMPLE
    .\run-app.ps1                 # build debug, then choose devices from a menu
    .\run-app.ps1 -NoBuild        # reuse the last built APK
    .\run-app.ps1 -Devices 1,3    # skip the menu (numbers as listed in the menu)
    .\run-app.ps1 -Devices all
#>
param(
    [switch]$NoBuild,
    [string[]]$Devices
)

$ErrorActionPreference = 'Stop'
$root      = $PSScriptRoot
$package   = 'com.EJ.ROvadiahYosefCalendar'
$activity  = 'com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity'
$apk       = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'

# ---- SDK tools ----
function Find-Sdk {
    $props = Join-Path $root 'local.properties'
    if (Test-Path $props) {
        $line = Get-Content $props | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
        if ($line) {
            $dir = ($line -split '=', 2)[1].Trim() -replace '\\:', ':' -replace '\\\\', '\'
            if (Test-Path $dir) { return $dir }
        }
    }
    foreach ($d in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA 'Android\Sdk'))) {
        if ($d -and (Test-Path $d)) { return $d }
    }
    throw 'Android SDK not found (set sdk.dir in local.properties or ANDROID_HOME).'
}
$sdk      = Find-Sdk
$adb      = Join-Path $sdk 'platform-tools\adb.exe'
$emulator = Join-Path $sdk 'emulator\emulator.exe'
if (-not (Test-Path $adb)) { throw "adb not found at $adb" }

function Get-Online {
    # serial -> description, for devices in the "device" state
    $map = [ordered]@{}
    foreach ($l in (& $adb devices -l | Select-Object -Skip 1)) {
        if ($l -match '^(\S+)\s+device\b(.*)$') {
            $serial = $Matches[1]
            $model  = if ($Matches[2] -match 'model:(\S+)') { $Matches[1] -replace '_', ' ' } else { '' }
            $map[$serial] = $model
        }
    }
    $map
}

function Get-AvdName([string]$serial) {
    if ($serial -notlike 'emulator-*') { return $null }
    $out = & $adb -s $serial emu avd name 2>$null
    if ($out) { return ($out | Select-Object -First 1).Trim() }
    $null
}

# ---- build the menu ----
& $adb start-server | Out-Null
$online  = Get-Online
$running = @{}
$choices = @()
foreach ($serial in $online.Keys) {
    $avd = Get-AvdName $serial
    if ($avd) { $running[$avd] = $serial }
    $label = if ($avd) { "$avd (running, $serial)" } else { "$($online[$serial]) ($serial)" }
    $choices += [pscustomobject]@{ Label = $label; Serial = $serial; Avd = $avd }
}
if (Test-Path $emulator) {
    foreach ($avd in (& $emulator -list-avds 2>$null)) {
        $avd = $avd.Trim()
        if ($avd -and $avd -notmatch '^INFO' -and -not $running.ContainsKey($avd)) {
            $choices += [pscustomobject]@{ Label = "$avd (emulator, will boot)"; Serial = $null; Avd = $avd }
        }
    }
}
if ($choices.Count -eq 0) { throw 'No connected devices and no emulators found.' }

if ($Devices) { $Devices = $Devices -split '[,\s]+' | Where-Object { $_ } }  # "-File" passes "1,3" as one string
if (-not $Devices) {
    Write-Host ''
    for ($i = 0; $i -lt $choices.Count; $i++) { Write-Host ("  [{0}] {1}" -f ($i + 1), $choices[$i].Label) }
    Write-Host ''
    $answer = Read-Host 'Devices to install on (e.g. 1,3 or "all")'
    $Devices = $answer -split '[,\s]+' | Where-Object { $_ }
}
$picked = @()
if ($Devices -contains 'all') { $picked = $choices }
else {
    foreach ($d in $Devices) {
        $n = 0
        if (-not [int]::TryParse($d, [ref]$n) -or $n -lt 1 -or $n -gt $choices.Count) { throw "Not a device number: $d" }
        $picked += $choices[$n - 1]
    }
}
if ($picked.Count -eq 0) { Write-Host 'Nothing selected.'; exit 0 }

# ---- boot emulators first, so they start while Gradle builds ----
$booting = @($picked | Where-Object { -not $_.Serial })
$before  = @((Get-Online).Keys)
foreach ($c in $booting) {
    Write-Host "Starting emulator $($c.Avd)..."
    Start-Process -FilePath $emulator -ArgumentList @('-avd', $c.Avd) -WindowStyle Minimized | Out-Null
}

# ---- build ----
if (-not $NoBuild) {
    Write-Host 'Building debug APK...'
    Push-Location $root
    try {
        & (Join-Path $root 'gradlew.bat') ':app:assembleDebug'
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit $LASTEXITCODE)." }
    } finally { Pop-Location }
}
if (-not (Test-Path $apk)) { throw "APK not found at $apk (run without -NoBuild)." }

# ---- wait for booting emulators to come online and finish booting ----
foreach ($c in $booting) {
    Write-Host "Waiting for $($c.Avd) to boot..." -NoNewline
    $deadline = (Get-Date).AddMinutes(5)
    while (-not $c.Serial) {
        if ((Get-Date) -gt $deadline) { break }
        foreach ($s in (Get-Online).Keys) {
            if ($before -notcontains $s -and (Get-AvdName $s) -eq $c.Avd) { $c.Serial = $s; break }
        }
        if (-not $c.Serial) { Start-Sleep -Seconds 2; Write-Host '.' -NoNewline }
    }
    if ($c.Serial) {
        while ((Get-Date) -lt $deadline) {
            if ((& $adb -s $c.Serial shell getprop sys.boot_completed 2>$null) -match '1') { break }
            Start-Sleep -Seconds 2; Write-Host '.' -NoNewline
        }
    }
    if ($c.Serial) { Write-Host " $($c.Serial)" } else { Write-Host ' timed out, skipping.' -ForegroundColor Yellow }
}

# ---- install and launch ----
$failed = 0
foreach ($c in ($picked | Where-Object { $_.Serial })) {
    $name = if ($c.Avd) { $c.Avd } else { $c.Label }
    Write-Host "Installing on $name..."
    & $adb -s $c.Serial install -r $apk
    if ($LASTEXITCODE -ne 0) { Write-Host "  install failed on $name" -ForegroundColor Red; $failed++; continue }
    & $adb -s $c.Serial shell am start -n "$package/$activity" | Out-Null
    if ($LASTEXITCODE -ne 0) { Write-Host "  launch failed on $name" -ForegroundColor Red; $failed++; continue }
    Write-Host "  launched on $name" -ForegroundColor Green
}
exit $failed
