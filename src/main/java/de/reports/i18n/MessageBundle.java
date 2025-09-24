package de.reports.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class MessageBundle {
    private static final String BUNDLE_NAME = "i18n.messages";
    private static ResourceBundle resourceBundle;
    private static Locale currentLocale = new Locale("de"); // Default German

    static {
        loadBundle();
    }

    private static void loadBundle() {
        try {
            resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
        } catch (MissingResourceException e) {
            System.err.println("Could not load resource bundle: " + BUNDLE_NAME);
            // Fallback to German
            resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, new Locale("de"));
        }
    }

    public static String getMessage(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            System.err.println("Missing resource key: " + key);
            return "!" + key + "!";
        }
    }

    public static String getMessage(String key, Object... args) {
        try {
            String message = resourceBundle.getString(key);
            return String.format(message, args);
        } catch (MissingResourceException e) {
            System.err.println("Missing resource key: " + key);
            return "!" + key + "!";
        }
    }

    public static void setLocale(Locale locale) {
        currentLocale = locale;
        loadBundle();
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void setLanguage(String languageCode) {
        Locale newLocale = new Locale(languageCode);
        setLocale(newLocale);
    }

    public static boolean isGerman() {
        return "de".equals(currentLocale.getLanguage());
    }

    public static boolean isEnglish() {
        return "en".equals(currentLocale.getLanguage());
    }
}