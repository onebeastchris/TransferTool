package dev.onechris.extension.transfertool;

import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.extension.ExtensionLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class LanguageManager {
    public static String DEFAULT_LOCALE = "en_us";
    private static final String EN_US_PROPERTIES = "en_US.properties";
    public static final Map<String, Properties> LOCALE_PROPERTIES = new HashMap<>();
    private final Config config;
    private final ExtensionLogger logger;

    public LanguageManager(Path languageFolder, Config config, ExtensionLogger logger) throws IOException {
        this.config = config;
        this.logger = logger;

        // Ensure it exists
        if (!languageFolder.toFile().exists()) {
            if (!languageFolder.toFile().mkdirs()) {
                throw new RuntimeException("Failed to create language folder!");
            }
        }

        List<Path> languageFiles;
        try {
            languageFiles = new ArrayList<>(Files.list(languageFolder).toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to list language files!", e);
        }

        if (config.defaultLocale() != null) {
            DEFAULT_LOCALE = config.defaultLocale().toLowerCase().replace(".properties", "");
        }

        //Check: Does default locale exist? Fallback to en_us if it does not.
        if (languageFiles.stream().noneMatch(path -> path.getFileName().toString().equalsIgnoreCase(DEFAULT_LOCALE + ".properties"))) {

            // Check: Is default locale not english?
            if (!DEFAULT_LOCALE.equalsIgnoreCase("en_us")) {
                logger.warning("Default configured locale " + DEFAULT_LOCALE + " not found, falling back to en_us.properties");
                DEFAULT_LOCALE = "en_us";
            }

            try (InputStream input = TransferTool.class.getClassLoader().getResourceAsStream(EN_US_PROPERTIES)) {
                assert input != null;
                Path defaultLocalePath = languageFolder.resolve(EN_US_PROPERTIES);
                Files.copy(input, defaultLocalePath, StandardCopyOption.REPLACE_EXISTING);
                languageFiles.add(defaultLocalePath);
            }
        }

        for (Path languageFile : languageFiles) {
            if (!languageFile.toFile().isFile()) {
                continue;
            }

            String fileName = languageFile.getFileName().toString();
            if (!fileName.endsWith(".properties")) {
                continue;
            }

            // Load the locale
            try (InputStream localeStream = Files.newInputStream(languageFile)) {
                Properties localeProp = new Properties();
                try (InputStreamReader reader = new InputStreamReader(localeStream, StandardCharsets.UTF_8)) {
                    localeProp.load(reader);
                } catch (Exception e) {
                    throw new AssertionError("Failed to load locale " + fileName);
                } finally {
                    localeStream.close();
                }

                // Insert the locale into the mappings, all lowercase
                LOCALE_PROPERTIES.put(fileName.substring(0, 5).toLowerCase(), localeProp);
            }
        }
    }

    public String getLocaleString(CommandSource source, String key) {
        return getLocaleString(source.locale(), key);
    }

    public String getLocaleString(String key) {
        return getLocaleString(config.defaultLocale(), key);
    }

    public String getLocaleString(String locale, String key) {
        String translation =  LOCALE_PROPERTIES.getOrDefault(locale.toLowerCase(), LOCALE_PROPERTIES.get(DEFAULT_LOCALE))
                .getProperty(key);
        if (translation != null) {
            return translation;
        } else {
            logger.warning("No translation fallback found for translation key: " + key);
            return key;
        }
    }
}