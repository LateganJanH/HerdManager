# HerdManager Android build script
# Sets JAVA_HOME to Android Studio JBR if not already set, then runs Gradle

$defaultJava = "C:\Program Files\Android\Android Studio\jbr"
if (-not $env:JAVA_HOME) {
    Write-Host "Setting JAVA_HOME to $defaultJava"
    $env:JAVA_HOME = $defaultJava
}

$javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
if (-not (Test-Path $javaExe)) {
    Write-Host "ERROR: Java not found at $env:JAVA_HOME" -ForegroundColor Red
    Write-Host "Please set JAVA_HOME to your JDK 17+ installation"
    exit 1
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $scriptDir
try {
    & .\gradlew.bat @args
} finally {
    Pop-Location
}
exit $LASTEXITCODE
