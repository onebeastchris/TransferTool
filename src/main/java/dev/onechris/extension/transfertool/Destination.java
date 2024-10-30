package dev.onechris.extension.transfertool;

import java.util.function.Consumer;

import static dev.onechris.extension.transfertool.TransferTool.logger;

public record Destination(String ip, int port) {

    public boolean invalidIp() {
        return this.ip == null || ip.isBlank();
    }

    public boolean invalidPort() {
        return port < 0 || port > 65535;
    }

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