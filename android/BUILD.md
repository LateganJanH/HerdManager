# Building HerdManager (Android)

## Quick start (Android Studio)

The easiest way to build is to **open the project in Android Studio**:
1. File → Open → select the `android` folder
2. Wait for Gradle sync
3. Build → Make Project (or Run)

Android Studio uses its bundled JDK and Gradle, so no extra setup is needed.

---

## Command-line build

### Option 1: Use the build script (recommended)

From the `android` folder, run:

```batch
build.bat assembleDebug
```

Or with PowerShell:

```powershell
.\build.ps1 assembleDebug
```

The script will try to find Java automatically (Android Studio JBR, Microsoft JDK, etc.).

### Option 2: Set JAVA_HOME manually

If the build fails with "JAVA_HOME is not set":

1. **Using Android Studio's JDK:**
   ```batch
   set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
   ```
   Or if Android Studio is in AppData:
   ```batch
   set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
   ```

2. **Or add to `gradle.properties`:**
   ```
   org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr
   ```

### Option 3: Fix Gradle wrapper (NoClassDefFoundError: IDownload)

If you see `NoClassDefFoundError: org/gradle/wrapper/IDownload`, the `gradle-wrapper.jar` may be outdated:

1. **Install Gradle** ( Chocolatey: `choco install gradle` or Scoop: `scoop install gradle`)
2. From the `android` folder, run:
   ```batch
   gradle wrapper --gradle-version=9.2.1
   ```
3. Run `build.bat assembleDebug` again

---

## Requirements

- **JDK 17+** (Android Studio bundles JDK 17)
- **Gradle 9.2.1** (downloaded automatically by the wrapper)
