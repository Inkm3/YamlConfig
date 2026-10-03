package example;

import com.github.inkm3.yamlconfig.YamlConfigs;
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine;
import com.github.inkm3.yamlconfig.source.PathYamlSource;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.descriptors.PrimitiveKind;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.descriptors.SerialDescriptorsKt;
import kotlinx.serialization.encoding.Decoder;
import kotlinx.serialization.encoding.Encoder;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JavaConsumer {
    public record Port(int value) { }

    // Explicit serializer for a Java record, not reflective automatic mapping.
    private static final class PortSerializer implements KSerializer<Port> {
        private final SerialDescriptor descriptor = SerialDescriptorsKt.PrimitiveSerialDescriptor(
            "example.Port", PrimitiveKind.INT.INSTANCE);
        @Override public SerialDescriptor getDescriptor() { return descriptor; }
        @Override public void serialize(Encoder encoder, Port port) { encoder.encodeInt(port.value()); }
        @Override public Port deserialize(Decoder decoder) { return new Port(decoder.decodeInt()); }
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("yamlconfig-published-consumer-");
        Path file = directory.resolve("config.yml");
        try {
            Files.writeString(file, "25565\n");
            var config = YamlConfigs.create(new SnakeYamlEngine(), new PathYamlSource(file), new PortSerializer());
            var session = config.load();
            if (session.getValue().value() != 25565) throw new AssertionError("load");
            session.setValue(new Port(30000));
            session.save();
            if (config.load().getValue().value() != 30000) throw new AssertionError("save/reload");
            if (!Files.readString(file).contains("30000")) throw new AssertionError("output");
            System.out.println("PUBLISHED_JAVA_CONSUMER_OK");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }
}
