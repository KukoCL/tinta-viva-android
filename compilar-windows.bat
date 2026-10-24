@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
call gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain
if errorlevel 1 (
    echo.
    echo No se pudo compilar. Revisa el error anterior y los requisitos del README.
    exit /b 1
)
echo.
echo APK listo en app\build\outputs\apk\debug\app-debug.apk
exit /b 0
