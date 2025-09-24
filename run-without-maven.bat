@echo off
echo JasperReports Desktop Application - Direct Run (Without Maven)
echo ===========================================================

REM Check Java version
echo Checking Java version...
java -version

REM Create lib directory if it doesn't exist
if not exist "lib" mkdir lib

echo.
echo NOTE: This project requires Maven to properly download dependencies.
echo Please install Maven from: https://maven.apache.org/download.cgi
echo.
echo Alternative: Use an IDE like IntelliJ IDEA or Eclipse that can handle Maven projects.
echo.

REM Try to compile without Maven (will likely fail due to missing dependencies)
echo Attempting to compile with available Java classpath...
javac -d target\classes -cp "src\main\java" src\main\java\de\reports\*.java src\main\java\de\reports\**\*.java

if errorlevel 1 (
    echo Compilation failed - Maven dependencies are required!
    echo.
    echo Please install Maven or use an IDE with Maven support.
    pause
    exit /b 1
)

echo Compilation successful! Attempting to run...
java -cp "target\classes" --module-path "lib" --add-modules javafx.controls,javafx.fxml,javafx.base de.reports.JasperReportsApp

pause