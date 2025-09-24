# JasperReports Desktop - Installationsanleitung

## Systemvoraussetzungen

### Java Development Kit (JDK)
- **Java 11 oder höher** ist erforderlich
- Empfohlen: OpenJDK 11 oder Oracle JDK 11+

#### Java Installation prüfen:
```cmd
java -version
javac -version
```

Falls Java nicht installiert ist:
1. Download: [OpenJDK](https://adoptium.net/) oder [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
2. Installieren und `JAVA_HOME` umgebungsvariable setzen
3. Java `bin`-Verzeichnis zu `PATH` hinzufügen

### Apache Maven
- **Maven 3.6 oder höher** ist erforderlich

#### Maven Installation prüfen:
```cmd
mvn --version
```

Falls Maven nicht installiert ist:
1. Download: [Apache Maven](https://maven.apache.org/download.cgi)
2. Entpacken in gewünschtes Verzeichnis (z.B. `C:\apache-maven-3.9.5`)
3. `MAVEN_HOME` umgebungsvariable setzen
4. Maven `bin`-Verzeichnis zu `PATH` hinzufügen

### JavaFX (für Entwicklung)
JavaFX ist als Maven-Dependency enthalten, aber für die Entwicklung wird empfohlen:
1. Download: [JavaFX SDK](https://openjfx.io/)
2. `PATH_TO_FX` umgebungsvariable setzen (für VS Code)

## Installation

### Option 1: Schnellstart mit Batch-Dateien (Windows)

1. **Projekt kompilieren:**
   ```cmd
   cd C:\Users\erhan\OneDrive\Masaüstü\jasper-reports-desktop
   build.bat
   ```

2. **Anwendung starten:**
   ```cmd
   run.bat
   ```

### Option 2: Manuelle Maven-Befehle

1. **In Projektverzeichnis wechseln:**
   ```cmd
   cd C:\Users\erhan\OneDrive\Masaüstü\jasper-reports-desktop
   ```

2. **Dependencies herunterladen:**
   ```cmd
   mvn dependency:resolve
   ```

3. **Projekt kompilieren:**
   ```cmd
   mvn clean compile
   ```

4. **Anwendung starten:**
   ```cmd
   mvn javafx:run
   ```

5. **Ausführbare JAR erstellen:**
   ```cmd
   mvn clean package
   ```

   Dann starten mit:
   ```cmd
   java --module-path "path/to/javafx/lib" --add-modules javafx.controls,javafx.fxml -jar target/jasper-reports-desktop-1.0.0.jar
   ```

### Option 3: Entwicklung in VS Code

1. **VS Code Extensions installieren:**
   - Extension Pack for Java
   - Maven for Java

2. **Projekt öffnen:**
   ```
   File → Open Folder → jasper-reports-desktop
   ```

3. **JavaFX konfigurieren:**
   - `PATH_TO_FX` umgebungsvariable setzen zu JavaFX lib-Ordner
   - Oder in `.vscode/launch.json` den Pfad anpassen

4. **Anwendung starten:**
   - `F5` drücken oder
   - Run → Start Debugging → "Launch JasperReports Desktop"

## Datenbankeinrichtung

### Für LEBO-Datenbank (bestehende Integration)
Die Anwendung ist bereits für Ihre LEBO-Datenbank vorkonfiguriert:

1. **SQL Server muss laufen**
2. **Datenbank**: `LebodoorsDB`
3. **Standard-Benutzer**: `LeboUser01`
4. **Verbindung testen** über die Anwendung

### Für andere Datenbanken
Unterstützte JDBC-Treiber sind bereits enthalten für:
- MySQL
- PostgreSQL
- Microsoft SQL Server
- Oracle Database

## Problembehandlung

### "mvn command not found"
```cmd
# Windows
set PATH=%PATH%;C:\apache-maven-3.9.5\bin

# Permanent in Systemumgebung hinzufügen
```

### "JAVA_HOME not set"
```cmd
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-11

# Permanent in Systemumgebung hinzufügen
```

### JavaFX Runtime-Fehler
```cmd
# Sicherstellen, dass JavaFX verfügbar ist
java --list-modules | findstr javafx
```

Falls JavaFX nicht verfügbar:
1. JavaFX SDK herunterladen
2. Als Maven-Dependency verwenden (bereits konfiguriert)
3. Oder `--module-path` beim Start angeben

### OutOfMemoryError
```cmd
# Heap-Size erhöhen
set MAVEN_OPTS=-Xmx2g
mvn javafx:run

# Oder beim direkten Start
java -Xmx2g -jar target/jasper-reports-desktop-1.0.0.jar
```

### Datenbankverbindung fehlschlägt
1. **SQL Server läuft:** Überprüfen Sie Windows Services
2. **Port ist offen:** Standard 1433 für SQL Server
3. **Firewall:** Erlauben Sie Verbindungen zu SQL Server
4. **Anmeldedaten:** Überprüfen Sie Benutzername/Passwort
5. **TCP/IP aktiviert:** In SQL Server Configuration Manager

### Compilation-Fehler
```cmd
# Cache löschen und neu kompilieren
mvn clean
mvn compile
```

## Erste Schritte nach der Installation

1. **Anwendung starten**
2. **LEBO-Datenbankvorlage wählen**
3. **Verbindungsdaten eingeben**
4. **Verbindung testen**
5. **Tabelle `merkmalstexte` auswählen**
6. **Ersten Bericht erstellen**

## Deinstallation

1. **Projektordner löschen:**
   ```
   C:\Users\erhan\OneDrive\Masaüstü\jasper-reports-desktop
   ```

2. **Anwendungsdaten löschen (optional):**
   ```
   %APPDATA%\JasperReportsDesktop
   ```

## Support

Bei Installationsproblemen:
1. Log-Dateien überprüfen
2. Java/Maven-Versionen verifizieren
3. Firewall/Antivirus-Einstellungen prüfen
4. Dokumentation erneut durchgehen

---

**Viel Erfolg mit JasperReports Desktop!** 🎉