package dev.onechris.extension.transfertool;

import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
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

public class TransferTool implements Extension {
    static ExtensionLogger logger;
    private Map<Destination, Destination> transferMappings = new HashMap<>();
    private Map<String, Destination> serverShortcuts = new HashMap<>();
    private Config config;
    private LanguageManager languageManager;

    @Subscribe
    public void onEnable(GeyserPreInitializeEvent ignored) {
        logger = this.logger();
        loadConfig();
        loadLanguageManager();
    }

    @Subscribe
    public void onReload(GeyserPreReloadEvent ignored) {
        logger.info("Reloading config!");
        loadConfig();
        loadLanguageManager();
    }

    @Subscribe
    public void onTransferEvent(ServerTransferEvent event) {
        logger().debug("TransferTool: Handling ServerTransferEvent: " + event.toString());

        Destination target = new Destination(event.host(), event.port());
        Destination bedrockTarget = transferMappings.get(target);
        if (bedrockTarget != null) {
            logger.debug(String.format("Transferring %s to %s based on transfer mapping", event.connection().name(), bedrockTarget));
            event.bedrockPort(bedrockTarget.port());
            event.bedrockHost(bedrockTarget.ip());
            return;
        } else if (config.forwardOriginalTarget()) {
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

        if (config.addTransferCommand()) {
            event.register("transfertool.command.transfer", TriState.TRUE);
            event.register("transfertool.command.transfer.any", TriState.NOT_SET);

            for (var shortcut : serverShortcuts.keySet()) {
                event.register("transfertool.shortcuts." + shortcut, TriState.TRUE);
            }
        }
    }

    @Subscribe
    public void onCommandRegister(GeyserDefineCommandsEvent event) {
        event.register(
                Command.builder(this)
                    .name("reload")
                    .description(languageManager.getLocaleString("commands.reload.desc"))
                    .permission("transfertool.command.reload")
                    .source(CommandSource.class)
                    .executor(($, $$, $$$) -> onReload(null))
                    .build()
        );

        if (config.addTransferCommand()) {
            event.register(
                Command.<GeyserConnection>builder(this)
                    .name("transfer")
                    .description(languageManager.getLocaleString("commands.transfer.desc"))
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
        if (!config.addTransferCommand()) {
            source.sendMessage(languageManager.getLocaleString(source, "commands.not_enabled"));
            return;
        }

        switch (args.length) {
            case 0 -> {
                if (transferMappings.isEmpty() && !mayTransferToAny(source)) {
                    source.sendMessage(languageManager.getLocaleString(source, "commands.transfer.none_available"));
                    return;
                }

                boolean mayTransferAny = mayTransferToAny(source);

                SimpleForm.Builder builder = SimpleForm.builder();

                builder.title(languageManager.getLocaleString(source, "menu.transfer.title"));

                for (String shortcut : serverShortcuts.keySet()) {
                    builder.optionalButton(shortcut, source.hasPermission("transfertool.shortcuts." + shortcut));
                }

                if (mayTransferAny) {
                    builder.button(languageManager.getLocaleString(source, "menu.transfer.custom"));
                }

                builder.validResultHandler((form, resp) -> {
                    // first: check if they chose the custom button?
                    if (mayTransferAny && resp.clickedButtonId() == form.buttons().size() - 1) {
                        source.sendForm(CustomForm.builder()
                                        .title(languageManager.getLocaleString(source, "menu.transfer.custom.title"))
                                        .input(languageManager.getLocaleString(source, "menu.transfer.custom.ip"),
                                                languageManager.getLocaleString(source, "form.custom.ip.placeholder"))
                                        .input(languageManager.getLocaleString(source, "menu.transfer.custom.port"),
                                                "19132", "19132")
                                        .validResultHandler(response ->
                                                tryParseAndTransferAny(response.asInput(), response.asInput(), source))
                                .build());
                        return;
                    }

                    // or: send 'em
                    transfer(source, serverShortcuts.get(resp.clickedButton().text()));
                });

                source.sendForm(builder);
            }
            case 1 -> {
                String arg = args[0];

                if (!source.hasPermission("transfertool.shortcuts." + arg)) {
                    source.sendMessage(languageManager.getLocaleString(source, "commands.transfer.no_permission")
                            .formatted(arg));
                    return;
                }

                Destination destination = serverShortcuts.get(arg);
                if (destination == null) {
                    if (mayTransferToAny(source)) {
                        destination = Destination.fromCombined(arg, 19132, source::sendMessage);
                    } else {
                        source.sendMessage(languageManager.getLocaleString(source, "commands.transfer.not_found")
                                .formatted(arg));
                        return;
                    }
                }

                transfer(source, destination);
            }
            case 2 -> {
                if (!mayTransferToAny(source)) {
                    source.sendMessage(languageManager.getLocaleString(source, "commands.transfer.too_many_args"));
                    return;
                }

                tryParseAndTransferAny(args[0], args[1], source);
            }
            default -> {
                source.sendMessage(languageManager.getLocaleString(source, "commands.transfer.unknown_args").formatted(args.length));
                source.sendMessage(languageManager.getLocaleString(source, "commands.transfer.args_provided").formatted(Arrays.toString(args)));
            }
        }
    }

    private void transfer(GeyserConnection source, Destination destination) {
        if (destination != null) {
            if (destination.invalidIp()) {
                source.sendMessage(languageManager.getLocaleString(source, "destination.ip.invalid"));
                return;
            }

            if (destination.invalidPort()) {
                source.sendMessage(languageManager.getLocaleString(source, "destination.port.invalid"));
                return;
            }
            source.transfer(destination.ip(), destination.port());
        } else {
            source.sendMessage(languageManager.getLocaleString(source, "destination.unknown"));
        }
    }

    private boolean mayTransferToAny(GeyserConnection connection) {
        return connection.hasPermission("transfertool.command.transfer.any");
    }

    private void tryParseAndTransferAny(String ip, String port, GeyserConnection source) {
        try {
            int parsed = Integer.parseInt(port);
            Destination destination = new Destination(ip, parsed);
            transfer(source, destination);
        } catch (NumberFormatException e) {
            source.sendMessage(languageManager.getLocaleString(source, "destination.port.invalid")
                    .formatted(port));
        }
    }

    private void loadLanguageManager() {
        try {
            languageManager = new LanguageManager(dataFolder().resolve("translations"), config, logger);
        } catch (Exception e) {
            logger.error("Unable to load TransferTool language manager! " + e.getMessage());
            this.disable();
        }
    }

    private void loadConfig() {
        try {
            config = ConfigLoader.loadConfig(this);
        } catch (Exception e) {
            logger.error("Unable to load TransferTool config! " + e.getMessage());
            this.disable();
            return;
        }

        Map<Destination, Destination> map = new HashMap<>();

        for (Map.Entry<String, String> entry : config.transferMappings().entrySet()) {
            map.put(Destination.fromCombined(entry.getKey(), 25565),
                    Destination.fromCombined(entry.getValue(), 19132));
        }

        logger.info("Registered %s transfer mappings.".formatted(map.size()));
        this.transferMappings = map;

        if (config.addTransferCommand()) {
            Map<String, Destination> shortcuts = new HashMap<>();
            for (Map.Entry<String, String> entry : config.transferShortcuts().entrySet()) {
                shortcuts.put(entry.getKey(), Destination.fromCombined(entry.getValue(), 19132));
            }

            logger.info("Registered %s server name mappings.".formatted(shortcuts.size()));
            this.serverShortcuts = shortcuts;
        }
    }
}
