# Build CallGuard.apk without Gradle.
#
# Uses only the Android SDK build-tools (aapt2, d8, zipalign, apksigner) and a
# JDK. No network access and no Gradle wrapper are required.
#
#   .\build-apk.ps1              # incremental build into .\build
#   .\build-apk.ps1 -Clean       # wipe .\build first
#
# The signed APK is copied to .\dist\CallGuard-<version>.apk

[CmdletBinding()]
param(
    [switch]$Clean,
    [string]$Version = '1.0',
    [int]$VersionCode = 1,
    [int]$MinSdk = 29,
    [int]$TargetSdk = 34
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$root = $PSScriptRoot
if (-not $root) { $root = (Get-Location).Path }
$main = Join-Path $root 'app\src\main'
$build = Join-Path $root 'build'
$dist = Join-Path $root 'dist'
$logFile = Join-Path $root 'build.log'

function Write-Step([string]$text) {
    Write-Host ''
    Write-Host "==> $text" -ForegroundColor Cyan
}

function Invoke-Tool([string]$file, [string[]]$arguments, [string]$what) {
    Write-Host "    $what"
    & $file @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$what failed with exit code $LASTEXITCODE"
    }
}

function Find-Sdk {
    $candidates = @()
    if ($env:ANDROID_HOME) { $candidates += $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT) { $candidates += $env:ANDROID_SDK_ROOT }
    $candidates += 'C:\Program Files (x86)\Android\android-sdk'
    $candidates += 'C:\Program Files\Android\android-sdk'
    if ($env:LOCALAPPDATA) { $candidates += (Join-Path $env:LOCALAPPDATA 'Android\Sdk') }
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path (Join-Path $candidate 'platforms'))) {
            return (Resolve-Path $candidate).Path
        }
    }
    throw 'Android SDK not found. Set ANDROID_HOME or install the SDK.'
}

function Find-BuildTools([string]$sdk) {
    $dir = Join-Path $sdk 'build-tools'
    $versions = Get-ChildItem $dir -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName 'aapt2.exe') } |
        Sort-Object { [version]($_.Name -replace '[^0-9.]', '') } -Descending
    if (-not $versions) { throw "No build-tools with aapt2 found in $dir" }
    return $versions[0].FullName
}

function Find-Platform([string]$sdk, [int]$target) {
    $dir = Join-Path $sdk 'platforms'
    $preferred = Join-Path $dir "android-$target\android.jar"
    if (Test-Path $preferred) { return $preferred }
    $any = Get-ChildItem $dir -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName 'android.jar') } |
        Sort-Object Name -Descending
    if (-not $any) { throw "No platform android.jar found in $dir" }
    return (Join-Path $any[0].FullName 'android.jar')
}

function Find-Jdk {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin') }
    $candidates += 'C:\Program Files (x86)\Android\openjdk\jdk-17.0.14\bin'
    $candidates += 'C:\Program Files\Android\openjdk\jdk-17.0.14\bin'
    $candidates += 'C:\Program Files\Android\jdk\jdk-8.0.302.8-hotspot\bin'
    if (Get-Command javac.exe -ErrorAction SilentlyContinue) {
        $candidates += (Split-Path -Parent (Get-Command javac.exe).Source)
    }
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path (Join-Path $candidate 'javac.exe'))) {
            return (Resolve-Path $candidate).Path
        }
    }
    throw 'JDK not found. Set JAVA_HOME to a JDK 17 installation.'
}

function Set-StoredEntry([string]$apkPath, [string]$entryName) {
    # Android 11+ requires resources.arsc to be stored uncompressed.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $read = [System.IO.Compression.ZipFile]::OpenRead($apkPath)
    try {
        $target = $read.Entries | Where-Object { $_.FullName -eq $entryName }
        if (-not $target) { throw "$entryName not found in $apkPath" }
        if ($target.CompressedLength -eq $target.Length) {
            return $false
        }
    } finally {
        $read.Dispose()
    }

    $tempPath = "$apkPath.tmp"
    if (Test-Path $tempPath) { Remove-Item $tempPath -Force }
    $source = [System.IO.Compression.ZipFile]::OpenRead($apkPath)
    try {
        $dest = [System.IO.Compression.ZipFile]::Open($tempPath, 'Create')
        try {
            foreach ($entry in $source.Entries) {
                $level = if ($entry.FullName -eq $entryName) {
                    [System.IO.Compression.CompressionLevel]::NoCompression
                } else {
                    [System.IO.Compression.CompressionLevel]::Optimal
                }
                $newEntry = $dest.CreateEntry($entry.FullName, $level)
                $newEntry.LastWriteTime = $entry.LastWriteTime
                $inputStream = $entry.Open()
                try {
                    $outputStream = $newEntry.Open()
                    try { $inputStream.CopyTo($outputStream) } finally { $outputStream.Dispose() }
                } finally {
                    $inputStream.Dispose()
                }
            }
        } finally {
            $dest.Dispose()
        }
    } finally {
        $source.Dispose()
    }
    Move-Item -Force $tempPath $apkPath
    return $true
}

function Add-DexToApk([string]$apkPath, [string]$dexPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::Open($apkPath, 'Update')
    try {
        $existing = $zip.GetEntry('classes.dex')
        if ($existing) { $existing.Delete() }
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zip, $dexPath, 'classes.dex',
            [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    } finally {
        $zip.Dispose()
    }
}

$sdk = Find-Sdk
$buildTools = Find-BuildTools $sdk
$androidJar = Find-Platform $sdk $TargetSdk
$jdkBin = Find-Jdk

$aapt2 = Join-Path $buildTools 'aapt2.exe'
$zipalign = Join-Path $buildTools 'zipalign.exe'
$d8Jar = Join-Path $buildTools 'lib\d8.jar'
$apksignerJar = Join-Path $buildTools 'lib\apksigner.jar'
$java = Join-Path $jdkBin 'java.exe'
$javac = Join-Path $jdkBin 'javac.exe'
$jar = Join-Path $jdkBin 'jar.exe'
$keytool = Join-Path $jdkBin 'keytool.exe'

Write-Host "SDK        : $sdk"
Write-Host "Build tools: $buildTools"
Write-Host "android.jar: $androidJar"
Write-Host "JDK        : $jdkBin"

if ($Clean -and (Test-Path $build)) {
    Remove-Item $build -Recurse -Force
}
foreach ($dir in @($build, $dist)) {
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
}

$resZip = Join-Path $build 'resources.zip'
$genDir = Join-Path $build 'gen'
$classesDir = Join-Path $build 'classes'
$dexDir = Join-Path $build 'dex'
$unsignedApk = Join-Path $build 'app-unsigned.apk'
$alignedApk = Join-Path $build 'app-aligned.apk'
$classesJar = Join-Path $build 'classes.jar'
$keystore = Join-Path $root 'keystore\debug.keystore'
$signedApk = Join-Path $build "CallGuard-$Version.apk"
$distApk = Join-Path $dist "CallGuard-$Version.apk"

foreach ($dir in @($genDir, $classesDir, $dexDir)) {
    if (Test-Path $dir) { Remove-Item $dir -Recurse -Force }
    New-Item -ItemType Directory -Path $dir | Out-Null
}
foreach ($file in @($resZip, $unsignedApk, $alignedApk, $classesJar)) {
    if (Test-Path $file) { Remove-Item $file -Force }
}

Write-Step 'Compiling resources (aapt2)'
Invoke-Tool $aapt2 @(
    'compile', '--dir', (Join-Path $main 'res'), '-o', $resZip
) 'aapt2 compile'

Write-Step 'Linking resources and manifest'
Invoke-Tool $aapt2 @(
    'link',
    '-o', $unsignedApk,
    '-I', $androidJar,
    '--manifest', (Join-Path $main 'AndroidManifest.xml'),
    '--java', $genDir,
    '--min-sdk-version', "$MinSdk",
    '--target-sdk-version', "$TargetSdk",
    '--version-code', "$VersionCode",
    '--version-name', $Version,
    '--auto-add-overlay',
    $resZip
) 'aapt2 link'

Write-Step 'Compiling Java sources'
$sources = @()
$sources += Get-ChildItem (Join-Path $main 'java') -Recurse -Filter '*.java' |
    Select-Object -ExpandProperty FullName
$sources += Get-ChildItem $genDir -Recurse -Filter '*.java' |
    Select-Object -ExpandProperty FullName
Write-Host "    $($sources.Count) source files"

$javacArgs = @(
    '-encoding', 'UTF-8',
    '-source', '8', '-target', '8',
    '-bootclasspath', $androidJar,
    '-d', $classesDir
)
& $javac @javacArgs @sources
if ($LASTEXITCODE -ne 0) {
    Write-Host '    retrying with --release 8' -ForegroundColor Yellow
    & $javac '-encoding' 'UTF-8' '--release' '8' '-classpath' $androidJar '-d' $classesDir @sources
    if ($LASTEXITCODE -ne 0) { throw 'javac failed' }
}

Write-Step 'Dexing (d8)'
Invoke-Tool $jar @('cf', $classesJar, '-C', $classesDir, '.') 'jar cf'
Invoke-Tool $java @(
    '-cp', $d8Jar, 'com.android.tools.r8.D8',
    '--release', '--min-api', "$MinSdk",
    '--lib', $androidJar,
    '--output', $dexDir,
    $classesJar
) 'd8'

Write-Step 'Packing classes.dex'
Add-DexToApk $unsignedApk (Join-Path $dexDir 'classes.dex')
if (Set-StoredEntry $unsignedApk 'resources.arsc') {
    Write-Host '    resources.arsc re-packed as stored'
}

Write-Step 'Aligning (zipalign)'
Invoke-Tool $zipalign @('-f', '-p', '4', $unsignedApk, $alignedApk) 'zipalign'

if (-not (Test-Path $keystore)) {
    Write-Step 'Creating debug keystore'
    $keystoreDir = Split-Path -Parent $keystore
    if (-not (Test-Path $keystoreDir)) {
        New-Item -ItemType Directory -Path $keystoreDir | Out-Null
    }
    Invoke-Tool $keytool @(
        '-genkeypair', '-v',
        '-keystore', $keystore,
        '-storetype', 'PKCS12',
        '-storepass', 'android',
        '-keypass', 'android',
        '-alias', 'androiddebugkey',
        '-keyalg', 'RSA',
        '-keysize', '2048',
        '-validity', '10000',
        '-dname', 'CN=CallGuard Debug,O=CallGuard,C=RU'
    ) 'keytool'
}

Write-Step 'Signing (apksigner)'
Invoke-Tool $java @(
    '-jar', $apksignerJar, 'sign',
    '--ks', $keystore,
    '--ks-key-alias', 'androiddebugkey',
    '--ks-pass', 'pass:android',
    '--key-pass', 'pass:android',
    '--min-sdk-version', "$MinSdk",
    '--v1-signing-enabled', 'false',
    '--v2-signing-enabled', 'true',
    '--v3-signing-enabled', 'true',
    '--out', $signedApk,
    $alignedApk
) 'apksigner sign'

Write-Step 'Verifying'
Invoke-Tool $java @('-jar', $apksignerJar, 'verify', '--verbose', $signedApk) 'apksigner verify'

Copy-Item -Force $signedApk $distApk
$info = Get-Item $distApk
$hash = (Get-FileHash $distApk -Algorithm SHA256).Hash.Substring(0, 16)

Write-Host ''
Write-Host "APK: $distApk" -ForegroundColor Green
Write-Host ("Size: {0:N0} KB   SHA-256: {1}..." -f ($info.Length / 1KB), $hash)
