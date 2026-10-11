package cc.unilock.nilcord.util;

import net.minecraft.util.StatCollector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class LangFileLoader {
    private static final Map<String, String> customTranslations = new HashMap<>();
    private static String currentLangFile = null;

    public static void loadLangFile(String langFileName) {
        customTranslations.clear();
        currentLangFile = langFileName;

        // Try loading from config directory first
        Path configLangPath = Paths.get("config", "nilcord", "lang", langFileName + ".lang");
        if (Files.exists(configLangPath)) {
            loadFromFile(configLangPath.toFile());
            return;
        }

        // Try loading from config directory without .lang extension
        configLangPath = Paths.get("config", "nilcord", "lang", langFileName);
        if (Files.exists(configLangPath)) {
            loadFromFile(configLangPath.toFile());
            return;
        }

        // Fallback to classpath resources
        try (InputStream inputStream = LangFileLoader.class.getClassLoader().getResourceAsStream("lang/" + langFileName + ".lang")) {
            if (inputStream == null) {
                inputStream = LangFileLoader.class.getClassLoader().getResourceAsStream(langFileName + ".lang");
            }
            if (inputStream == null) {
                inputStream = LangFileLoader.class.getClassLoader().getResourceAsStream(langFileName);
            }

            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;

                        int equalsIndex = line.indexOf('=');
                        if (equalsIndex > 0) {
                            String key = line.substring(0, equalsIndex).trim();
                            String value = line.substring(equalsIndex + 1).trim();
                            customTranslations.put(key, value);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    customTranslations.put(key, value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String translate(String key) {
        if (customTranslations.containsKey(key)) {
            return customTranslations.get(key);
        }
        return StatCollector.translateToLocal(key);
    }

    public static boolean hasCustomTranslation(String key) {
        return customTranslations.containsKey(key);
    }

    public static String getCurrentLangFile() {
        return currentLangFile;
    }

    public static void clear() {
        customTranslations.clear();
        currentLangFile = null;
    }
}