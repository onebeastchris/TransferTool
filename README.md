# TransferTool

A Geyser extension that allows you to map Java -> Geyser/Bedrock servers to transfer Bedrock players to.

# Introduction
Since 1.20.5/6, there is a native Java transfer packet. For once, this is a feature that Bedrock already did have! 
However, there is one notable difference: Geyser receives the Java client IP/port to transfer players to. If Geyser
just passes that to the Bedrock client, the client likely would not find a Bedrock server there[^1].
In most cases, this would not happen. Hence, this Geyser extension allows you to customize which Geyser - or Bedrock server -
to transfer players to upon Geyser receives a Java transfer packet.


[^1]: Technically it could work if Geyser is also running on the same Java port, but that's usually not the case.

# Configuration
There are two main configuration options:
- `forward-original-target` (boolean; possible values true/false): <br>
    Forwards the IP/port combination received from the Java server directly to the Bedrock client. 
This would only be useful in the case where the Java server already accounts for the receiver to be a Geyser player,
and is already sending a Geyser server IP/port to connect to.

- `transfer-mappings` (Map, String -> String) <br>
    Maps a Java IP/port combination to a Bedrock IP/port combination. To represent a IP/address combination, 
use the following format: `example.com:12892`, or `198.51.100.0:13132`. Alternatively, you can leave out the port,
this would fall back to the default Java/Bedrock ports (25565 for Java, and 19132 for Bedrock).

Further, there is an option to add a Bedrock-only `/transfertool transfer` command. If enabled, Bedrock players can:
- Transfer to pre-defined servers (set in `transfer-shortcuts`) by running '/transfertool transfer <server>'
- Transfer to any server - if they have the `transfertool.command.transfer.any` permission. Usage:
`/transfertool transfer test.geysermc.org` or `/transfertool transfer test.geysermc.org:19132` or `/transfertool transfer test.geysermc.org 19132`
- See a pretty menu to select their options by running `/transfertool transfer` command without arguments.

- `default-locale`: See the "localization" section below for info on this config option.

Example config:
```yaml
# TransferTool Configuration
# 
# Documentation: https://onechris.dev/docs/extensions/transfertool/
# Source: https://github.com/onebeastchris/TransferTool/

# Whether to pass the IP/Port sent by the Java server to the Bedrock client.
# This option will only work properly if the Java server already accounts for Geyser clients
# when sending transfer requests.
forward-original-target: false

# A map of Java IP/Port combinations to Geyser IP/Port combinations.
# If you do not specify a port with a ":<port>" addition,
# it will use the default ports for Bedrock/Java respectively.
transfer-mappings:
    127.0.0.1:25565: 127.0.0.1:19132
    javaip.com: bedrockip.com

# Whether to add a '/transfertool transfer <server>' command to the server that can only be used by Bedrock players.
# This uses the "server-names" below. Command permission: "transfertool.command.transfer".

# Alternatively, users can transfer to any IP and Port combination if they additionally have the
# "transfertool.command.transfer.any" permission.

# Further, if ran without any server-name or IP/Port combination, it will open a menu.
add-transfer-command: false

# Allows configuring shortcuts for servers to use in the optional '/transfertool transfer <server>' command.
# Has no use while "add-transfer-command" is false.
#
# To restrict who can connect to which server, you can remove permissions from users.
# By default, all shortcuts are available to all players. The permissions follow the following format:
# "transfertool.shortcuts.<server>"
#
# For example, for the shortcut "lobby", it would be "transfertool.shortcuts.lobby".
transfer-shortcuts:
    lobby: bedrockip.com
    vanilla: 127.0.0.1:19132

# The default locale. For example, TransferTool's transfer shortcuts menu can be localized.
default-locale: en_US

# The config version. DO NOT CHANGE!
version: 1
```

To add more transfer mappings, create a new line - just like in the example.
Note for ipv6 addresses, you'll need to wrap the ipv6 with square brackets (see [here](https://en.wikipedia.org/wiki/IPv6_address#Literal_IPv6_addresses_in_network_resource_identifiers))

Note:
If you enable `forward-original-target` and also provide server mappings, the server mappings will be checked first (and override the Java IP/port provided with the mapped Bedrock destination).

> [!TIP]
> If you're making use of `transfer-shortcuts`, or are enabling the transfer command,
> and are using Geyser on a platform without a default permissions system (e.g. Geyser-Velocity, -BungeeCord, and -Fabric),
> I'd recommend also installing [LuckLink](https://github.com/onebeastchris/LuckLink) to allow automatic registration of permissions.

# Installation
To install this Geyser extension, download the TransferTool.jar from the Releases tab, and add it to Geyser's extension folder.
One restart later, and this extension is ready to be used! To reload the config, you can use Geyser's reload command (`/geyser reload`).

# Localization
This extension has multi-language support. However, the default language is English (default-locale in the config) - to add more languages, copy the en_US.properties file from the `/extensions/transfertool/translations` folder, and translate the strings. 
Then, save the file (e.g. de_DE.properties for German). You can select a default locale in the config.yml. Otherwise, all locales will be loaded & used automatically if a player's language matches. 
To see all possible locales, check the Geyser locales [here](https://github.com/GeyserMC/languages/tree/master/texts).

# Getting Help
Help is provided via discord: https://discord.gg/WdmrRHRJhS. Alternatively, if you see errors or run into complications, feel free to open an issue here! Feature requests can also be made that way.