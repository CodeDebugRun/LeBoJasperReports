package de.reports;

/**
 * Launcher class to avoid JavaFX runtime issues
 * This class acts as a simple main class wrapper that calls the JavaFX Application
 */
public class Launcher {
    public static void main(String[] args) {
        // Launch the JavaFX Application
        JasperReportsApp.main(args);
    }
}