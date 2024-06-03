package net.onebeastchris.extension.transfertool;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.java.ServerTransferEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreReloadEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TransferTool implements Extension {
    private static ExtensionLogger logger;
    private Path configPath;
    private Map<Destination, Destination> transferMappings = new HashMap<>();
    private boolean followJavaTransfer = false;

    @Subscribe
    public void onEnable(GeyserPostInitializeEvent ignored) {
        logger = this.logger();
        configPath = this.dataFolder().resolve("transfertool.conf");
        loadConfig();
    }

    @Subscribe
    public void onReload(GeyserPreReloadEvent ignored) {
        logger.info("Reloading config!");
        loadConfig();
    }

    @Subscribe
    public void onTransferEvent(ServerTransferEvent event) {
        logger().debug("TransferTool: Handling ServerTransferEvent: " + event.toString());

        Destination target = new Destination(event.host(), event.port());

        Destination bedrockTarget = transferMappings.get(target);
        if (bedrockTarget != null) {
            logger.debug(String.format("Transferring %s to %s based on transfer mapping", event.connection().name(), bedrockTarget));
            event.bedrockPort(bedrockTarget.port);
            event.bedrockHost(bedrockTarget.ip);
            return;
        } else if (followJavaTransfer) {
            logger.debug(String.format("Transferring %s to %s due to java transfer forwarding", event.connection().name(), bedrockTarget));
            event.bedrockHost(event.host());
            event.bedrockPort(event.port());
            return;
        }

        logger.debug(String.format("No target found for Java server %s:%s", event.host(), event.port()));
    }

    private void loadConfig() {
        TransferToolConfig config;
        final HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .path(configPath)
                .defaultOptions(configurationOptions -> configurationOptions.header("TranferTool Configuration"))
                .prettyPrinting(true)
                .build();

        try {
            final CommentedConfigurationNode node = loader.load();
            config = node.get(TransferToolConfig.class);
            loader.save(node);
        } catch (ConfigurateException e) {
            this.logger().error("Could not load config! " + e.getMessage());
            this.logger().error("Disabling TransferTool...");
            e.printStackTrace();
            this.disable();
            return;
        }

        if (config == null) {
            this.logger().error("Could not load config! Disabling...");
            this.disable();
            return;
        }

        this.followJavaTransfer = config.isForwardOriginalTarget();
        HashMap<Destination, Destination> map = new HashMap<>();

        for (Map.Entry<String, String> entry : config.getTransferMappings().entrySet()) {
            map.put(Destination.fromCombined(entry.getKey(), 25565),
                    Destination.fromCombined(entry.getValue(), 19132));
        }

        this.transferMappings = map;
    }

    private record Destination(String ip, int port) {
        public static Destination fromCombined(String input, int fallbackPort) {
            String ip = input;
            int port = fallbackPort;

            if (input.contains(":")) {
                String parsePort;
                if (input.startsWith("[")) {
                    // Handle IPv6 addresses... since Bedrock apparently technically supports these?
                    int closingBracketIndex = input.indexOf("]");
                    ip = input.substring(1, closingBracketIndex);
                    parsePort = input.substring(closingBracketIndex + 2);
                } else {
                    // Handle IPv4 addresses or domain names
                    int colonIndex = input.lastIndexOf(":");
                    ip = input.substring(0, colonIndex);
                    parsePort = input.substring(colonIndex + 1);
                }

                try {
                    port = Integer.parseInt(parsePort);
                } catch (NumberFormatException e) {
                    TransferTool.logger.error("Invalid port found: " + parsePort + " in input: " + input + "! " +
                            "Defaulting to default port (" + fallbackPort + ").");
                }
            }

            return new Destination(ip, port);
        }

        @Override
        public String toString() {
            return ip + ":" + port;
        }
    }
}
