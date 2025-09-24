package de.reports.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * Create directory if it doesn't exist
     */
    public static boolean createDirectoryIfNotExists(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info("Directory created: {}", dirPath);
                return true;
            }
            return true;
        } catch (IOException e) {
            logger.error("Failed to create directory: {}", dirPath, e);
            return false;
        }
    }

    /**
     * Write text to file
     */
    public static boolean writeToFile(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);
            // Create parent directories if they don't exist
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("Content written to file: {}", filePath);
            return true;
        } catch (IOException e) {
            logger.error("Failed to write to file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Read text from file
     */
    public static String readFromFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                logger.warn("File does not exist: {}", filePath);
                return "";
            }
            return new String(Files.readAllBytes(path));
        } catch (IOException e) {
            logger.error("Failed to read from file: {}", filePath, e);
            return "";
        }
    }

    /**
     * Copy file from source to destination
     */
    public static boolean copyFile(String sourcePath, String destinationPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path destination = Paths.get(destinationPath);

            // Create parent directories if they don't exist
            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }

            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            logger.info("File copied from {} to {}", sourcePath, destinationPath);
            return true;
        } catch (IOException e) {
            logger.error("Failed to copy file from {} to {}", sourcePath, destinationPath, e);
            return false;
        }
    }

    /**
     * Delete file
     */
    public static boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("File deleted: {}", filePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Check if file exists
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Get file extension
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Get file name without extension
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }

    /**
     * Get file size in bytes
     */
    public static long getFileSize(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.size(path);
            }
            return 0;
        } catch (IOException e) {
            logger.error("Failed to get file size: {}", filePath, e);
            return 0;
        }
    }

    /**
     * List files in directory with specific extension
     */
    public static List<String> listFilesWithExtension(String directoryPath, String extension) {
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            return paths.filter(Files::isRegularFile)
                       .filter(path -> path.toString().toLowerCase().endsWith("." + extension.toLowerCase()))
                       .map(Path::toString)
                       .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to list files in directory: {}", directoryPath, e);
            return List.of();
        }
    }

    /**
     * Get user home directory
     */
    public static String getUserHomeDirectory() {
        return System.getProperty("user.home");
    }

    /**
     * Get application data directory
     */
    public static String getAppDataDirectory() {
        String userHome = getUserHomeDirectory();
        String appName = "JasperReportsDesktop";

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return Paths.get(userHome, "AppData", "Roaming", appName).toString();
        } else if (os.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", appName).toString();
        } else {
            return Paths.get(userHome, "." + appName.toLowerCase()).toString();
        }
    }

    /**
     * Ensure app data directory exists and return it
     */
    public static String ensureAppDataDirectory() {
        String appDataDir = getAppDataDirectory();
        createDirectoryIfNotExists(appDataDir);
        return appDataDir;
    }

    /**
     * Get temp directory path
     */
    public static String getTempDirectory() {
        String tempDir = System.getProperty("java.io.tmpdir");
        String appTempDir = Paths.get(tempDir, "JasperReportsDesktop").toString();
        createDirectoryIfNotExists(appTempDir);
        return appTempDir;
    }

    /**
     * Clean temp directory (remove old files)
     */
    public static void cleanTempDirectory() {
        String tempDir = getTempDirectory();
        try (Stream<Path> paths = Files.walk(Paths.get(tempDir))) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> {
                     try {
                         // Delete files older than 24 hours
                         return System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis() > 86400000;
                     } catch (IOException e) {
                         return false;
                     }
                 })
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         logger.warn("Failed to delete temp file: {}", path, e);
                     }
                 });
            logger.info("Temp directory cleaned: {}", tempDir);
        } catch (IOException e) {
            logger.error("Failed to clean temp directory: {}", tempDir, e);
        }
    }

    /**
     * Validate file path for security
     */
    public static boolean isValidFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        // Check for path traversal attacks
        String normalizedPath = filePath.replace('\\', '/');
        return !normalizedPath.contains("../") && !normalizedPath.contains("..\\");
    }

    /**
     * Get safe file name (remove illegal characters)
     */
    public static String getSafeFileName(String fileName) {
        if (fileName == null) {
            return "report";
        }

        // Remove illegal characters for file names
        return fileName.replaceAll("[<>:\"/\\|?*]", "_")
                      .replaceAll("\\s+", "_")
                      .trim();
    }
}