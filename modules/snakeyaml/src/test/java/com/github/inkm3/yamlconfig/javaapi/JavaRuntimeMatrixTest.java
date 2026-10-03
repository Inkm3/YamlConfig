package com.github.inkm3.yamlconfig.javaapi;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.List;
import com.github.inkm3.yamlconfig.YamlConfigs;
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JavaRuntimeMatrixTest {
    @Test void gradleRunsTestsOnTheRequestedJavaRuntime() {
        String expected = System.getProperty("yamlconfig.expectedTestJavaVersion");
        assertNotNull(expected, "Run via Gradle so the test JVM is explicitly configured");
        assertEquals(Integer.parseInt(expected), Runtime.version().feature());
    }

    @Test void publishedClassesStillTargetJava17() throws Exception {
        for (Class<?> type : List.of(YamlConfigs.class, SnakeYamlEngine.class)) {
            String resource = "/" + type.getName().replace('.', '/') + ".class";
            try (InputStream stream = type.getResourceAsStream(resource)) {
                assertNotNull(stream, resource);
                DataInputStream input = new DataInputStream(stream);
                assertEquals(0xCAFEBABE, input.readInt(), resource);
                input.readUnsignedShort();
                assertEquals(61, input.readUnsignedShort(), "Expected Java 17 bytecode: " + resource);
            }
        }
    }
}
