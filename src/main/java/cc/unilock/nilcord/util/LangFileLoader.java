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
    private static final Map<String, Map<String, String>> modTranslations = new HashMap<>();
    private static final Map<String, String> globalTranslations = new HashMap<>();
    private static String currentLangFile = null;

    public static void loadLangFile(String langFileName) {
        globalTranslations.clear();
        currentLangFile = langFileName;

        // Try loading from config directory first
        Path configLangPath = Paths.get("config", "nilcord", "lang", langFileName + ".lang");
        if (Files.exists(configLangPath)) {
            loadFromFile(configLangPath.toFile(), globalTranslations);
            return;
        }

        // Try loading from config directory without .lang extension
        configLangPath = Paths.get("config", "nilcord", "lang", langFileName);
        if (Files.exists(configLangPath)) {
            loadFromFile(configLangPath.toFile(), globalTranslations);
            return;
        }

        // Fallback to classpath resources
        InputStream inputStream = LangFileLoader.class.getClassLoader().getResourceAsStream("lang/" + langFileName + ".lang");
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
                        globalTranslations.put(key, value);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadModLangFile(String modId) {
        Map<String, String> translations = new HashMap<>();
        
        // Try loading from config directory first
        Path configLangPath = Paths.get("config", "nilcord", "lang", modId + ".lang");
        if (Files.exists(configLangPath)) {
            loadFromFile(configLangPath.toFile(), translations);
            modTranslations.put(modId, translations);
            return;
        }

        // Try loading from config directory without .lang extension
        configLangPath = Paths.get("config", "nilcord", "lang", modId);
        if (Files.exists(configLangPath)) {
            loadFromFile(configLangPath.toFile(), translations);
            modTranslations.put(modId, translations);
            return;
        }

        // Fallback to classpath resources
        InputStream inputStream = LangFileLoader.class.getClassLoader().getResourceAsStream("lang/" + modId + ".lang");
        if (inputStream == null) {
            inputStream = LangFileLoader.class.getClassLoader().getResourceAsStream(modId + ".lang");
        }
        if (inputStream == null) {
            inputStream = LangFileLoader.class.getClassLoader().getResourceAsStream(modId);
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
                        translations.put(key, value);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            modTranslations.put(modId, translations);
        }
    }

    private static void loadFromFile(File file, Map<String, String> translations) {
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
                    translations.put(key, value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String translate(String key) {
        return translate(key, null);
    }

    public static String translate(String key, String modId) {
        if (modId != null && modTranslations.containsKey(modId)) {
            Map<String, String> translations = modTranslations.get(modId);
            if (translations.containsKey(key)) {
                return translations.get(key);
            }
        }
        if (globalTranslations.containsKey(key)) {
            return globalTranslations.get(key);
        }
        return StatCollector.translateToLocal(key);
    }

    public static boolean hasCustomTranslation(String key) {
        return hasCustomTranslation(key, null);
    }

    public static boolean hasCustomTranslation(String key, String modId) {
        if (modId != null && modTranslations.containsKey(modId)) {
            Map<String, String> translations = modTranslations.get(modId);
            if (translations.containsKey(key)) {
                return true;
            }
        }
        return globalTranslations.containsKey(key);
    }

    public static String getCurrentLangFile() {
        return currentLangFile;
    }

    public static void clear() {
        modTranslations.clear();
        globalTranslations.clear();
        currentLangFile = null;
    }

    public static void loadAllModLangFiles(java.util.List<String> modIds) {
        for (String modId : modIds) {
            if (modId != null && !modId.isEmpty()) {
                loadModLangFile(modId.trim());
            }
        }
    }
}