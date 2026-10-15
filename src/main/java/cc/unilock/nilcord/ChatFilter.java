package cc.unilock.nilcord;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class ChatFilter {
    private static final Logger LOGGER = LogManager.getLogger("nilcord");

    private static final List<Pattern> BLOCKED_PATTERNS = new ArrayList<>();
    private static File filterFile;

    public static void init(File configDir) {
        filterFile = new File(configDir, "nilcord_chat_filter.txt");

        if (!filterFile.exists()) {
            createDefaultFile();
        }

        loadFromFile();
    }

    private static void createDefaultFile() {
        try {
            filterFile.createNewFile();
            PrintWriter writer = new PrintWriter(new FileWriter(filterFile));
            writer.println("# Nilcord Chat Filter");
            writer.println("# Plain words will be hardened automatically");
            writer.println("# Prefix with regex: to use raw regex");
            writer.println("discord.gg");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadFromFile() {
        BLOCKED_PATTERNS.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(filterFile))) {

            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                try {

                    if (line.startsWith("regex:")) {
                        // Raw regex mode
                        String raw = line.substring(6);
                        BLOCKED_PATTERNS.add(Pattern.compile(raw, Pattern.CASE_INSENSITIVE));
                    } else {
                        // Word mode (auto-hardened)
                        String hardened = regexify(line.toLowerCase(Locale.ROOT));
                        BLOCKED_PATTERNS.add(Pattern.compile(hardened));
                    }

                } catch (Exception e) {
                    System.out.println("Invalid filter entry: " + line);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String regexify(String string) {

        StringBuilder neue = new StringBuilder("(?i)");

        for (char c : string.toCharArray()) {

            switch (c) {
                case 'a': neue.append("[a]"); break;
                case 'c': neue.append("[c]"); break;
                case 'e': neue.append("[e]"); break;
                case 'i': neue.append("[i]"); break;
                case 'j': neue.append("[j]"); break;
                case 'm': neue.append("[m]"); break;
                case 'n': neue.append("[n]"); break;
                case 'o': neue.append("[o]"); break;
                case 's': neue.append("[s]"); break;
                case 'u': neue.append("[u]"); break;
                default: neue.append(Pattern.quote(String.valueOf(c)));
            }
        }

        return neue.toString();
    }

    public static String normalize(String input) {

        if (input == null) return "";

        String text = input.toLowerCase(Locale.ROOT);

        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("\\p{M}", "");

        // Kill non-English characters
        text = text.replaceAll("[^a-z0-9 ]", "");

        text = text.replaceAll("(.)\\1{2,}", "$1$1");

        return text;
    }

    public static boolean isBlocked(String message) {

        if (message == null) return false;
        
        String clean = normalize(message);

        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(clean).find()) {
                LOGGER.debug("Chat message blocked by pattern '{}': {}", pattern.pattern(), message);
                return true;
            }
        }

        return false;
    }
}
