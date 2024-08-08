# TransferTool

A Geyser extension that allows you to map Java -> Geyser/Bedrock servers to transfer Bedrock players to.

# Introduction
Since 1.20.5/6, there is a native Java transfer packet. For once, this is a feature that Bedrock already did have! 
However, there is one notable difference: Geyser receives the Java client IP/port to transfer players to. If Geyser
just passes that to the Bedrock client, the client likely would not find a Bedrock server there*.
In most cases, this would not happen. Hence, this Geyser extension allows you to customize which Geyser - or Bedrock server -
to transfer players to upon Geyser receives a Java transfer packet.


*unless Geyser is also running on the same Java port, but that's not always the case.

# Configuration
There are currently two configuration options:
- `forwardOriginalTarget` (boolean; possible values true/false): <br>
    Forwards the IP/port combination received from the Java server directly to the Bedrock client. 
This would only be useful in the case where the Java server already accounts for the receiver to be a Geyser player,
and is already sending a Geyser server IP/port to connect to.

- `transferMappings` (Map, String -> String) <br>
    Maps a Java IP/port combination to a Bedrock IP/port combination. To represent a IP/address combination, 
use the following format: `example.com:12892`, or `198.51.100.0:13132`. Alternatively, you can leave out the port,
this would fall back to the default Java/Bedrock ports (25565 for Java, and 19132 for Bedrock).

Example config:
```
# TranferTool Configuration

# Whether to pass the IP/port sent by the Java server to the Bedrock client.
# This option will only work properly if the Java server already accounts for Geyser clients
# when sending transfer requests.
forward-original-target=false

# A map of Java IP/port combinations to Geyser IP/port combinations.
# If you do not specify a port with a ":<port>" addition,
# it will use the default ports for Bedrock/Java respectively.
transfer-mappings {
    "127.0.0.1:25565"="127.0.0.1:19132"
    "javaip.com"="bedrockip.com"
}
```

To add more transfer mappings, create a new line - just like in the example.
Note for ipv6 addresses, you'll need to wrap the ipv6 with square brackets (see [here](https://en.wikipedia.org/wiki/IPv6_address#Literal_IPv6_addresses_in_network_resource_identifiers))

Note:
If you enable `forward-original-target` and also provide server mappings, the server mappings will be checked first (and override the Java IP/port provided with the mapped Bedrock destination).

# Installation
To install this Geyser extension, download the TransferTool.jar from the Releases tab, and add it to Geyser's extension folder.
One restart later, and this extension is ready to be used! To reload the config, you can use Geyser's reload command (`/geyser reload`).

# Getting Help
Help is provided via discord: https://discord.gg/WdmrRHRJhS. Alternatively, if you see errors or run into complications, feel free to open an issue here! Feature requests can also be made that way.