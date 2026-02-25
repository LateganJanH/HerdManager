# Fix missing or broken Gradle wrapper JAR.
# Run from the android folder: .\fix-gradle-wrapper.ps1
# Requires: PowerShell 5.1+ and internet access.

$ErrorActionPreference = "Stop"
$wrapperDir = Join-Path $PSScriptRoot "gradle\wrapper"
$propsPath = Join-Path $wrapperDir "gradle-wrapper.properties"
$jarPath = Join-Path $wrapperDir "gradle-wrapper.jar"

if (-not (Test-Path $propsPath)) {
    Write-Error "Not found: $propsPath. Run this script from the android folder."
}

# Parse distributionUrl to get Gradle version (e.g. gradle-9.2.1-bin.zip -> 9.2.1)
$content = Get-Content $propsPath -Raw
if ($content -match 'gradle-(\d+\.\d+(?:\.\d+)?)-bin\.zip') {
    $version = $Matches[1]
} else {
    $version = "9.2.1"
    Write-Warning "Could not detect version from gradle-wrapper.properties; using $version"
}

# Official distributions URL returns 404 for some versions (e.g. 9.2.1); fallback to Gradle GitHub.
$urlPrimary = "https://services.gradle.org/distributions/gradle-$version-wrapper.jar"
$urlFallback = "https://github.com/gradle/gradle/raw/v$version/gradle/wrapper/gradle-wrapper.jar"

$downloaded = $false
try {
    Write-Host "Trying $urlPrimary ..."
    Invoke-WebRequest -Uri $urlPrimary -OutFile $jarPath -UseBasicParsing
    $downloaded = $true
} catch {
    if ($_.Exception.Response.StatusCode.Value__ -eq 404) {
        if (Test-Path $jarPath) { Remove-Item $jarPath -Force }
        Write-Host "Not found (404). Trying GitHub fallback ..."
        try {
            Invoke-WebRequest -Uri $urlFallback -OutFile $jarPath -UseBasicParsing
            $downloaded = $true
        } catch {
            Write-Error "Fallback failed: $($_.Exception.Message)"
        }
    } else {
        if (Test-Path $jarPath) { Remove-Item $jarPath -Force }
        Write-Error "Download failed: $($_.Exception.Message)"
    }
}
if (-not $downloaded) {
    Write-Error "Could not download gradle-wrapper.jar"
}

if (-not (Test-Path $jarPath)) {
    Write-Error "Download did not create $jarPath"
}

$size = (Get-Item $jarPath).Length
Write-Host "Saved gradle-wrapper.jar ($([math]::Round($size/1KB)) KB). Run: .\gradlew.bat :app:assembleDebug"
exit 0
