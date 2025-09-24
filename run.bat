@echo off
echo JasperReports Desktop Application - Start Script
echo ================================================

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Apache Maven from https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Check Java version
echo Checking Java version...
java -version

REM Check if compiled
if not exist "target\classes" (
    echo Application not compiled. Compiling now...
    call mvn compile
    if errorlevel 1 (
        echo Compilation failed! Please check the error messages above.
        pause
        exit /b 1
    )
)

echo Starting JasperReports Desktop Application...
echo.
call mvn javafx:run

if errorlevel 1 (
    echo Application failed to start! Please check the error messages above.
    pause
    exit /b 1
)

pause