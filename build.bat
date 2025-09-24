@echo off
echo JasperReports Desktop Application - Build Script
echo ================================================

REM Check if Maven is installed
mvn --version > nul 2>&1
if errorlevel 1 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Apache Maven from https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Clean and compile
echo Cleaning previous builds...
call mvn clean

echo Compiling application...
call mvn compile

if errorlevel 1 (
    echo Build failed! Please check the error messages above.
    pause
    exit /b 1
)

echo Build successful!
echo.
echo To run the application, use:
echo   mvn javafx:run
echo.
echo To create an executable JAR, use:
echo   mvn package
echo.
pause