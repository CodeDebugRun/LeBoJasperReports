# JasperReports Desktop Anwendung

Eine moderne, benutzerfreundliche Java Desktop-Anwendung für die Erstellung von Berichten mit JasperReports Community Edition.

## 🎯 Überblick

Diese Anwendung bietet eine intuitive grafische Benutzeroberfläche für:
- **Datenbankverbindungen** (MySQL, PostgreSQL, SQL Server, Oracle)
- **Tabellen- und Spaltenauswahl**
- **Bericht-Design** mit Sortierung, Filterung und Gruppierung
- **Format-Konfiguration** (Seitenlayout, Schriftarten, Farben)
- **Export** in verschiedene Formate (PDF, Excel, Word, HTML, CSV)

## 🔧 Voraussetzungen

- **Java 11 oder höher**
- **Maven 3.6 oder höher**
- Zugang zu einer unterstützten Datenbank

## 🚀 Installation und Start

### 1. Projekt kompilieren
```bash
cd jasper-reports-desktop
mvn clean compile
```

### 2. Abhängigkeiten installieren
```bash
mvn dependency:resolve
```

### 3. Anwendung starten
```bash
mvn javafx:run
```

### 4. Ausführbare JAR erstellen
```bash
mvn clean package
java -jar target/jasper-reports-desktop-1.0.0.jar
```

## 💾 Unterstützte Datenbanken

- **MySQL** (Port 3306)
- **PostgreSQL** (Port 5432)
- **Microsoft SQL Server** (Port 1433)
- **Oracle Database** (Port 1521)

### LEBO-Datenbank Integration

Die Anwendung enthält eine vordefinierte Vorlage für Ihre bestehende LEBO-Datenbank:
- **Server**: localhost
- **Port**: 1433
- **Datenbank**: LebodoorsDB
- **Typ**: Microsoft SQL Server

## 🖥️ Benutzeroberfläche

### 1. Datenbankverbindung
- Wählen Sie eine Vorlage oder geben Sie Verbindungsdaten manuell ein
- Testen Sie die Verbindung vor dem Fortfahren
- Speichern Sie häufig verwendete Verbindungen

### 2. Tabellen-Auswahl
- Durchsuchen Sie verfügbare Tabellen und Views
- Betrachten Sie Spaltenstrukturen
- Sehen Sie sich Datenvorschauen an

### 3. Bericht-Design
- Wählen Sie Spalten für den Bericht aus
- Konfigurieren Sie Sortierung und Filter
- Erstellen Sie Gruppierungen und Berechnungen

### 4. Format und Stil
- Wählen Sie Seitenformat (A4, A3, Letter)
- Konfigurieren Sie Schriftarten und Farben
- Fügen Sie Kopf- und Fußzeilen hinzu

### 5. Vorschau und Export
- Betrachten Sie eine Vorschau des Berichts
- Exportieren Sie in gewünschte Formate
- Speichern Sie Berichte lokal

## 🌐 Sprachunterstützung

- **Deutsch** (Standard)
- **Englisch** (optional)

Sprachwechsel über: Ansicht → Sprache

## 📁 Verzeichnisstruktur

```
jasper-reports-desktop/
├── src/main/java/de/reports/
│   ├── JasperReportsApp.java          # Hauptklasse
│   ├── gui/                           # GUI-Komponenten
│   │   ├── MainWindow.java
│   │   ├── DatabaseConnectionPanel.java
│   │   ├── TableSelectionPanel.java
│   │   └── ...
│   ├── database/                      # Datenbank-Management
│   │   ├── DatabaseManager.java
│   │   ├── DatabaseConnectionInfo.java
│   │   └── ...
│   ├── i18n/                         # Internationalisierung
│   └── utils/                        # Hilfsfunktionen
├── src/main/resources/
│   ├── i18n/                         # Sprachdateien
│   ├── config/                       # Konfiguration
│   ├── styles/                       # CSS-Styling
│   └── templates/                    # JasperReports-Vorlagen
└── pom.xml                           # Maven-Konfiguration
```

## 🔧 Konfiguration

### Anwendungseinstellungen
Standardkonfiguration in `src/main/resources/config/application.json`:

```json
{
  "application": {
    "defaultLanguage": "de",
    "supportedLanguages": ["de", "en"]
  },
  "ui": {
    "windowWidth": 1200,
    "windowHeight": 800
  },
  "database": {
    "connectionTimeout": 30000,
    "queryTimeout": 60000
  },
  "reports": {
    "defaultPageSize": "A4",
    "defaultOrientation": "PORTRAIT",
    "maxRecordsPerReport": 50000
  }
}
```

### Datenbankvorlagen
Vordefinierte Verbindungen in `src/main/resources/config/database_templates.json`

## 📊 Berichtsfunktionen

### Unterstützte Berichtstypen
- **Einfache Listen** mit Sortierung und Filterung
- **Gruppierte Berichte** mit Zwischensummen
- **Zusammenfassende Berichte** mit Berechnungen

### Export-Formate
- **PDF** - Für Drucken und Archivierung
- **Excel (XLSX)** - Für weitere Datenanalyse
- **Word (DOCX)** - Für Dokumentation
- **HTML** - Für Web-Präsentation
- **CSV** - Für Datenaustausch

## 🔒 Sicherheit

- **Verschlüsselte Passwort-Speicherung**
- **Sichere Datenbankverbindungen**
- **SQL-Injection-Schutz**
- **Verbindungs-Timeouts**

## 🐛 Problembehandlung

### Häufige Probleme

1. **"Anwendung startet nicht"**
   - Überprüfen Sie Java-Version (min. Java 11)
   - Stellen Sie sicher, dass JavaFX verfügbar ist

2. **"Datenbankverbindung fehlgeschlagen"**
   - Prüfen Sie Netzwerkverbindung
   - Verifizieren Sie Anmeldedaten
   - Überprüfen Sie Firewall-Einstellungen

3. **"OutOfMemoryError bei großen Berichten"**
   - Reduzieren Sie die Anzahl der Datensätze
   - Erhöhen Sie die JVM-Heap-Größe: `-Xmx2g`

4. **"CSS-Styles laden nicht"**
   - Überprüfen Sie Pfad zu `application.css`
   - Verifizieren Sie Berechtigung für Ressourcen-Dateien

### Log-Dateien

Logs werden in folgenden Verzeichnissen gespeichert:
- **Windows**: `%APPDATA%\JasperReportsDesktop\logs\`
- **macOS**: `~/Library/Application Support/JasperReportsDesktop/logs/`
- **Linux**: `~/.jasperreportsdesktop/logs/`

### Debug-Modus aktivieren

```bash
java -Dlogging.level.de.reports=DEBUG -jar jasper-reports-desktop-1.0.0.jar
```

## 🤝 Integration mit bestehendem LEBO-Projekt

Diese JasperReports-Anwendung ist vollständig kompatibel mit Ihrer bestehenden LEBO-Datenbank:

1. Verwenden Sie die "LEBO Database"-Vorlage
2. Geben Sie Ihre LEBO-Anmeldedaten ein
3. Verbinden Sie sich mit der `merkmalstexte`-Tabelle
4. Erstellen Sie Berichte basierend auf Ihren vorhandenen Daten

## 📝 Entwicklung

### Build-Prozess
```bash
# Kompilieren
mvn compile

# Tests ausführen
mvn test

# Paket erstellen
mvn package

# Alle Dependencies installieren
mvn clean install
```

### IDE-Konfiguration
Für die Entwicklung mit VS Code:
- Java Extension Pack installieren
- JavaFX Runtime hinzufügen
- VM-Arguments für JavaFX konfigurieren

## 📄 Lizenz

Dieses Projekt steht unter der MIT-Lizenz.

## 🆘 Support

Bei Fragen oder Problemen:
1. Überprüfen Sie die Dokumentation
2. Schauen Sie in die Log-Dateien
3. Öffnen Sie ein Issue im Repository

---

**Version**: 1.0.0
**Letzte Aktualisierung**: Oktober 2024
**Kompatibel mit**: Java 11+, JavaFX 19+, JasperReports 6.20+