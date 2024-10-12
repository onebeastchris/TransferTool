package dev.onechris.extension.transfertool;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.java.ServerTransferEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreReloadEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.util.TriState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TransferTool implements Extension {
    private static ExtensionLogger logger;
    private Map<Destination, Destination> transferMappings = new HashMap<>();
    private Map<String, Destination> serverShortcuts = new HashMap<>();
    private boolean followJavaTransfer = false;
    private boolean registerTransferCommand = false;

    @Subscribe
    public void onEnable(GeyserPreInitializeEvent ignored) {
        logger = this.logger();
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

    @Subscribe
    public void registerPermissions(GeyserRegisterPermissionsEvent event) {
        event.register("transfertool.command.reload", TriState.NOT_SET);

        if (registerTransferCommand) {
            event.register("transfertool.command.transfer", TriState.TRUE);
            event.register("transfertool.command.transfer.any", TriState.NOT_SET);
        }
    }

    @Subscribe
    public void onCommandRegister(GeyserDefineCommandsEvent event) {
        event.register(
                Command.builder(this)
                    .name("reload")
                    .description("Reloads the TransferTool configuration")
                    .permission("transfertool.command.reload")
                    .source(CommandSource.class)
                    .executor(($, $$, $$$) -> onReload(null))
                    .build()
        );

        if (registerTransferCommand) {
            event.register(
                Command.<GeyserConnection>builder(this)
                    .name("transfer")
                    .description("Transfers you to a another server.")
                    .bedrockOnly(true)
                    .playerOnly(true)
                    .permission("transfertool.command.transfer")
                    .source(GeyserConnection.class)
                    .executor(this::handleArgs)
                .build()
            );
        }
    }

    private void handleArgs(GeyserConnection source, Command command, String[] args) {
        // May occur if someone changes the config to no longer register the command.
        // Commands cannot be unregistered :/
        if (!registerTransferCommand) {
            source.sendMessage("This command is not enabled!");
            return;
        }

        switch (args.length) {
            case 0 -> {
                source.sendMessage("No server specified! Correct usage: '/transfertool transfer <server>'");
                if (mayTransferToAny(source)) {
                    source.sendMessage("Alternatively, transfer to any server with '/transfertool transfer <ip> <port>'");
                }
            }
            case 1 -> {
                String arg = args[0];
                Destination destination = serverShortcuts.get(arg);

                if (destination == null) {
                    if (mayTransferToAny(source)) {
                        destination = Destination.fromCombined(arg, 19132, source::sendMessage);
                    } else {
                        source.sendMessage("Unknown server %s!".formatted(arg));
                        return;
                    }
                }

                transfer(source, destination);
            }
            case 2 -> {
                if (!mayTransferToAny(source)) {
                    source.sendMessage("Too many arguments! Correct usage: '/transfertool transfer <server>'");
                    return;
                }

                try {
                    int port = Integer.parseInt(args[1]);
                    Destination destination = new Destination(args[0], port);
                    transfer(source, destination);
                } catch (NumberFormatException e) {
                    source.sendMessage("Invalid port! %s".formatted(args[1]));
                }
            }
            default -> {
                source.sendMessage("Received too many arguments (%s)!".formatted(args.length));
                source.sendMessage("Provided args: %s".formatted(Arrays.toString(args)));
            }
        }
    }

    private void transfer(GeyserConnection source, Destination destination) {
        if (destination != null) {
            if (destination.ip == null || destination.ip.isBlank()) {
                source.sendMessage("Empty IP provided!");
                return;
            }
            source.transfer(destination.ip, destination.port);
        } else {
            source.sendMessage("Unknown destination!");
        }
    }

    private boolean mayTransferToAny(GeyserConnection connection) {
        return connection.hasPermission("transfertool.command.transfer.any");
    }

    private void loadConfig() {
        Config config;
        try {
            config = ConfigLoader.loadConfig(this);
        } catch (Exception e) {
            logger.error("Unable to load TransferTool config! " + e.getMessage());
            this.disable();
            return;
        }

        this.followJavaTransfer = config.isForwardOriginalTarget();
        Map<Destination, Destination> map = new HashMap<>();

        for (Map.Entry<String, String> entry : config.getTransferMappings().entrySet()) {
            map.put(Destination.fromCombined(entry.getKey(), 25565),
                    Destination.fromCombined(entry.getValue(), 19132));
        }

        logger.info("Registered %s transfer mappings.".formatted(map.size()));
        this.transferMappings = map;
        this.registerTransferCommand = config.isAddTransferCommand();

        if (registerTransferCommand) {
            Map<String, Destination> shortcuts = new HashMap<>();
            for (Map.Entry<String, String> entry : config.getTransferShortcuts().entrySet()) {
                shortcuts.put(entry.getKey(), Destination.fromCombined(entry.getValue(), 19132));
            }

            logger.info("Registered %s server name mappings.".formatted(shortcuts.size()));
            this.serverShortcuts = shortcuts;
        }
    }

    private record Destination(String ip, int port) {

        public static Destination fromCombined(String input, int fallbackPort) {
            return fromCombined(input, fallbackPort, logger::error);
        }

        public static Destination fromCombined(String input, int fallbackPort, Consumer<String> caller) {
            String ip = input;
            int port = fallbackPort;

            if (input.contains(":")) {
                String parsePort;
                if (input.startsWith("[")) {
                    // Handle IPv6 addresses... since Bedrock technically supports these?
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
                    caller.accept("Invalid port found: " + parsePort + " in input: " + input + "! " +
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
