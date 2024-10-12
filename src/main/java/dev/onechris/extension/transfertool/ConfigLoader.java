package dev.onechris.extension.transfertool;

import org.geysermc.geyser.api.extension.*;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader {

    private static final String HEADER = """
        TransferTool Configuration
        
        Documentation: https://onechris.dev/docs/extensions/transfertool/
        Source: https://github.com/onebeastchris/TransferTool/
        """;

    public static Config loadConfig(Extension extension) throws IOException {
        Files.createDirectories(extension.dataFolder());

        Path oldPath = extension.dataFolder().resolve("transfertool.conf");
        ExtensionLogger logger = extension.logger();
        Config oldConfig = null;

        boolean mustMigrate = oldPath.toFile().exists();

        if (mustMigrate) {
            logger.info("Starting TransferTool config migration...");

            final HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                    .path(oldPath)
                    .defaultOptions(configurationOptions -> configurationOptions.header(HEADER))
                    .prettyPrinting(true)
                    .build();

            try {
                final CommentedConfigurationNode node = loader.load();
                oldConfig = node.get(Config.class);
            } catch (ConfigurateException e) {
                logger.warning("Unable to read old config!" + e.getMessage());
            }

            oldPath.toFile().deleteOnExit();
        }

        final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(extension.dataFolder().resolve("config.yml"))
                .nodeStyle(NodeStyle.BLOCK)
                .defaultOptions(configurationOptions -> configurationOptions.header(HEADER))
                .build();

        final CommentedConfigurationNode node = loader.load();

        // Load the config with old values if available
        Config config;
        if (mustMigrate && oldConfig != null) {
            config = node.get(Config.class, oldConfig);
        } else {
            config = node.get(Config.class);
        }

        if (config == null) {
            throw new IllegalStateException("config is null!");
        }

        CommentedConfigurationNode newNode = CommentedConfigurationNode.root(loader.defaultOptions());
        newNode.set(Config.class, config);

        loader.save(newNode);
        return config;
    }
}

