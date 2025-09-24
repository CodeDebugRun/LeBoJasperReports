@echo off
echo Testing JasperReports Desktop Application - Simple Test
echo =====================================================

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Apache Maven from https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java JDK 11 or higher
    pause
    exit /b 1
)

echo Java version:
java -version

echo.
echo Maven version:
mvn --version

echo.
echo Cleaning and compiling...
mvn clean compile

if %ERRORLEVEL% NEQ 0 (
    echo Build failed! Check the error messages above.
    pause
    exit /b 1
)

echo.
echo Starting simple test...
mvn javafx:run

pause