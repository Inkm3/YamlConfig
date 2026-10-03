package com.github.inkm3.yamlconfig.javaapi;

import com.github.inkm3.yamlconfig.YamlConfigs;
import com.github.inkm3.yamlconfig.serialization.YamlSerialization;
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine;
import com.github.inkm3.yamlconfig.source.ClasspathYamlInput;
import com.github.inkm3.yamlconfig.source.PathYamlSource;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javaapi.JavaServerConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JavaSourceConvenienceTest {
    @Test void pathOnlyConstructorLoadsAndSavesAnActualFile() throws Exception {
        Path directory = Files.createTempDirectory("yaml-java-path-");
        Path file = directory.resolve("config.yml");
        try {
            var config = YamlConfigs.create(new SnakeYamlEngine(), new PathYamlSource(file), JavaServerConfig.Companion.serializer());
            var session = config.load();
            assertFalse(Files.exists(file));
            session.setValue(new JavaServerConfig("server", 30000, List.of()));
            session.save();
            assertEquals(30000, config.load().getValue().getPort());
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    @Test void existingFullConstructorKeepsExplicitCharsetBehavior() throws Exception {
        Path file = Files.createTempFile("yaml-java-charset-", ".yml");
        try {
            Files.writeString(file, "port: 30000\n", StandardCharsets.UTF_16LE);
            var config = YamlConfigs.create(new SnakeYamlEngine(),
                new PathYamlSource(file, StandardCharsets.UTF_16LE), JavaServerConfig.Companion.serializer());
            assertEquals(30000, config.load().getValue().getPort());
        } finally { Files.deleteIfExists(file); }
    }

    @Test void classpathConstructorWithoutCharsetDefaultsToUtf8() throws Exception {
        ClasspathYamlInput input = new ClasspathYamlInput(getClass().getClassLoader(), "java-api/defaults.yml");
        try (Reader reader = input.openReader()) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[128];
            int read;
            while ((read = reader.read(buffer)) != -1) result.append(buffer, 0, read);
            assertEquals("port: 30000\n", result.toString());
        }
    }

    @Test void serializationStaticAccessPreservesTheCompanionGetter() {
        assertSame(YamlSerialization.Companion.getDefault(), YamlSerialization.getDefault());
        assertNotNull(new YamlSerialization());
        assertNotNull(new YamlSerialization(YamlSerialization.getDefault().getSerializersModule()));
        assertFalse(new YamlSerialization(YamlSerialization.getDefault().getSerializersModule(), false).getEncodeDefaults());
    }
}
