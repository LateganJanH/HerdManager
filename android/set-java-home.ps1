# Set JAVA_HOME for the current PowerShell session (Windows).
# Run: .\set-java-home.ps1
# Or to find your JDK first: .\set-java-home.ps1 -ShowOnly

param([switch]$ShowOnly)

$paths = @(
    "C:\Program Files\Android\Android Studio\jbr",
    "C:\Program Files\Android\Android Studio\jre",
    "C:\Program Files\Eclipse Adoptium\jdk-17*",
    "C:\Program Files\Microsoft\jdk-17*",
    "C:\Program Files\Java\jdk-17*",
    "C:\Program Files\Java\jdk-21*"
)

$found = $null
foreach ($p in $paths) {
    $resolved = $null
    if ($p -match '\*') {
        $parent = $p -replace '\*.*', ''
        if (Test-Path $parent) {
            $resolved = Get-ChildItem -Path $parent -Directory -Filter ($p -split '\\')[-1] -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
        }
    } elseif (Test-Path $p) {
        $resolved = (Get-Item $p).FullName
    }
    if ($resolved -and (Test-Path (Join-Path $resolved "bin\java.exe"))) {
        $found = $resolved
        break
    }
}

if (-not $found) {
    Write-Host "No JDK found in common locations. Set JAVA_HOME manually, e.g.:" -ForegroundColor Yellow
    Write-Host '  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"' -ForegroundColor Cyan
    Write-Host "To find Gradle JDK in Android Studio: File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK" -ForegroundColor Gray
    exit 1
}

if ($ShowOnly) {
    Write-Host $found
    exit 0
}

$env:JAVA_HOME = $found
Write-Host "JAVA_HOME set to: $found" -ForegroundColor Green
Write-Host "This is for the current session only. To build: .\gradlew.bat :app:assembleDebug" -ForegroundColor Gray
