@echo off
echo JasperReports Desktop Application - Direct Run
echo ===============================================

REM Check if JAR exists
if not exist "target\jasper-reports-desktop-1.0.0.jar" (
    echo Error: JAR file not found! Please run 'mvn clean package -DskipTests' first.
    pause
    exit /b 1
)

echo Starting JasperReports Desktop Application...
echo.

REM Run with proper JavaFX arguments to suppress warnings
java --add-exports java.base/sun.nio.ch=ALL-UNNAMED ^
     --add-exports java.desktop/sun.awt=ALL-UNNAMED ^
     --add-opens java.base/java.lang=ALL-UNNAMED ^
     --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
     --add-opens java.base/java.util=ALL-UNNAMED ^
     --add-opens java.desktop/javax.swing=ALL-UNNAMED ^
     -Dprism.order=sw ^
     -Djava.awt.headless=false ^
     -jar target\jasper-reports-desktop-1.0.0.jar

if errorlevel 1 (
    echo Application exited with an error. Please check the log messages above.
    pause
    exit /b 1
)

echo Application closed successfully.
pause