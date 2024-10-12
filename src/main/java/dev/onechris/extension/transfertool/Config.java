package dev.onechris.extension.transfertool;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Map;

@Getter
@ConfigSerializable
public class Config {

    @Comment("""
             Whether to pass the IP/Port sent by the Java server to the Bedrock client.
             This option will only work properly if the Java server already accounts for Geyser clients
             when sending transfer requests.
            """)
    private boolean forwardOriginalTarget = false;

    @Comment("""
             A map of Java IP/Port combinations to Geyser IP/Port combinations.
             If you do not specify a port with a ":<port>" addition,
             it will use the default ports for Bedrock/Java respectively.
            """)
    private Map<String, String> transferMappings = Map.of(
            "127.0.0.1:25565", "127.0.0.1:19132",
            "javaip.com", "bedrockip.com");

    @Comment("""
             Whether to add a '/transfertool transfer <server>' command to the server that can only be used by Bedrock players.
             This uses the "server-names" below. Command permission: "transfertool.command.transfer".
            
             Alternatively, users can transfer to any IP and Port combination if they additionally have the
             "transfertool.command.transfer.any" permission.
            """)
    private boolean addTransferCommand = false;

    @Comment("""
             Allows configuring shortcuts for servers to use in the optional '/transfertool transfer <server>' command.
             Has no use while "add-transfer-command" is false.
            """)
    private Map<String, String> transferShortcuts = Map.of(
            "vanilla", "127.0.0.1:19132",
            "lobby", "bedrockip.com"
    );

    @Comment("""
            The config version. DO NOT CHANGE!
            """)
    private int version = 1;
}
