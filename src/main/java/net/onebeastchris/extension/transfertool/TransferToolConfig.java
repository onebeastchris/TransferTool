package net.onebeastchris.extension.transfertool;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Map;

@ConfigSerializable
public class TransferToolConfig {

    @Comment("""
             Whether to pass the IP/Port sent by the Java server to the Bedrock client.
             This option will only work properly if the Java server already accounts for Geyser clients
             when sending transfer requests.
            """)
    private boolean forwardOriginalTarget = false;

    @Comment("""
             A map of Java ip/port combinations to Geyser ip/port combinations.
             If you do not specify a port with a ":<port>" addition,
             it will use the default ports for Bedrock/Java respectively.
            """)
    private Map<String, String> transferMappings = Map.of(
            "127.0.0.1:25565", "127.0.0.1:19132",
            "javaip.com", "bedrockip.com");

    public boolean isForwardOriginalTarget() {
        return forwardOriginalTarget;
    }

    public Map<String, String> getTransferMappings() {
        return transferMappings;
    }
}
