@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
"%~dp0runtime\bin\java.exe" -Xmx1g --enable-native-access=ALL-UNNAMED -cp "app;lib/*" Main
pause
endlocal
