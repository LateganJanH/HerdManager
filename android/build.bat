@echo off
REM HerdManager Android build script
REM Sets JAVA_HOME if not already set, then runs Gradle

cd /d "%~dp0"

if not "%JAVA_HOME%"=="" goto check_java

REM Try Android Studio bundled JDK (most common for Android dev)
if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
    goto check_java
)
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    goto check_java
)
REM Try Microsoft/Adoptium JDK
for /d %%D in ("C:\Program Files\Microsoft\jdk-17*" "C:\Program Files\Java\jdk-17*") do (
    if exist "%%D\bin\java.exe" (
        set "JAVA_HOME=%%D"
        goto check_java
    )
)

echo ERROR: JAVA_HOME is not set and no JDK 17+ was found.
echo.
echo Set JAVA_HOME manually, e.g.:
echo   set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
echo.
echo Or add org.gradle.java.home to gradle.properties:
echo   org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr
exit /b 1

:check_java
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JAVA_HOME points to invalid directory: %JAVA_HOME%
    exit /b 1
)

call gradlew.bat %*
set EXIT=%ERRORLEVEL%
if %EXIT% neq 0 (
    echo.
    echo If you see NoClassDefFoundError: org/gradle/wrapper/IDownload, run:
    echo   gradle wrapper --gradle-version=9.2.1
    echo after installing Gradle ^(choco install gradle^). Or open the project in Android Studio.
)
exit /b %EXIT%
