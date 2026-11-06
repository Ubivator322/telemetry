@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul

if not exist "lib" mkdir "lib"

if not exist "lib\sqlite-jdbc-3.53.4.0.jar" (
    echo Downloading sqlite-jdbc...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.53.4.0/sqlite-jdbc-3.53.4.0.jar' -OutFile 'lib\sqlite-jdbc-3.53.4.0.jar'"
    if errorlevel 1 (
        echo Failed to download sqlite-jdbc
        pause
        exit /b 1
    )
)

if not exist "lib\json-20240303.jar" (
    echo Downloading org.json...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar' -OutFile 'lib\json-20240303.jar'"
    if errorlevel 1 (
        echo Failed to download org.json
        pause
        exit /b 1
    )
)

if not exist "out" mkdir "out"

set "JAVAC="
set "JAVA="

where javac >nul 2>nul
if not errorlevel 1 (
    set "JAVAC=javac"
    set "JAVA=java"
)

if not defined JAVAC (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\javac.exe" (
            set "JAVAC=%JAVA_HOME%\bin\javac.exe"
            set "JAVA=%JAVA_HOME%\bin\java.exe"
        )
    )
)

if not defined JAVAC (
    for /d %%D in ("%USERPROFILE%\.jdks\*") do (
        if exist "%%D\bin\javac.exe" (
            set "JAVAC=%%D\bin\javac.exe"
            set "JAVA=%%D\bin\java.exe"
        )
    )
)

if not defined JAVAC (
    echo JDK not found. Install JDK or run Main from IntelliJ IDEA.
    pause
    exit /b 1
)

"%JAVAC%" -encoding UTF-8 -cp "lib/*" -d out src\*.java
if errorlevel 1 (
    echo Compilation error
    pause
    exit /b 1
)

"%JAVA%" -Xmx1g -cp "out;lib/*" Main
pause
endlocal
