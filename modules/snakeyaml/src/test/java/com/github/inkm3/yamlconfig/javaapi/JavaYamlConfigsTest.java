package com.github.inkm3.yamlconfig.javaapi;

import com.github.inkm3.yamlconfig.YamlConfig;
import com.github.inkm3.yamlconfig.YamlConfigSession;
import com.github.inkm3.yamlconfig.YamlConfigs;
import com.github.inkm3.yamlconfig.YamlSerializedConfigsKt;
import com.github.inkm3.yamlconfig.exception.YamlWriteException;
import com.github.inkm3.yamlconfig.save.YamlSaveMode;
import com.github.inkm3.yamlconfig.serialization.YamlSerialization;
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine;
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy;
import java.util.List;
import javaapi.JavaApiSerializers;
import javaapi.JavaMutableConfig;
import javaapi.JavaServerConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JavaYamlConfigsTest {
    private final SnakeYamlEngine engine = new SnakeYamlEngine();

    @Test void factoryUsesGeneratedCompanionSerializerAndJavaAccessors() {
        JavaMemorySource source = new JavaMemorySource("server-name: lobby\nport: 25565\n", true);
        YamlConfig<JavaServerConfig> config = YamlConfigs.create(engine, source, JavaServerConfig.Companion.serializer());
        assertEquals(0, source.opens);
        YamlConfigSession<JavaServerConfig> session = config.load();
        assertEquals("lobby", session.getValue().getName());
        session.setValue(new JavaServerConfig("creative", 30000, List.of("localhost")));
        session.save();
        assertEquals(session.getValue(), config.load().getValue());
        assertEquals(1, source.commits);
    }

    @Test void kotlinFacadeSignatureRemainsCallableFromJava() {
        JavaMemorySource source = new JavaMemorySource("port: 30000\n", true);
        YamlConfig<JavaServerConfig> config = YamlSerializedConfigsKt.yamlConfig(
            engine, source, JavaServerConfig.Companion.serializer(), null,
            YamlSaveMode.PRESERVE_OVERRIDES, YamlMissingFilePolicy.DO_NOT_CREATE,
            YamlSerialization.getDefault());
        assertEquals(30000, config.load().getValue().getPort());
    }

    @Test void noChangeAndRepeatedSaveDoNotRewrite() {
        JavaMemorySource source = new JavaMemorySource("port: +25565 # keep\n", true);
        YamlConfigSession<JavaServerConfig> session = YamlConfigs.create(
            engine, source, JavaServerConfig.Companion.serializer()).load();
        session.save();
        assertEquals(0, source.writes);
        session.setValue(new JavaServerConfig("server", 30000, List.of()));
        session.save();
        String saved = source.text;
        session.save();
        assertEquals(saved, source.text);
        assertEquals(1, source.commits);
        assertTrue(saved.contains("# keep"));
    }

    @Test void mutableSetterChangesUseTheExistingDetachedBaseline() {
        JavaMemorySource source = new JavaMemorySource("port: 1\n", true);
        YamlConfig<JavaMutableConfig> config = YamlConfigs.create(engine, source, JavaMutableConfig.Companion.serializer());
        YamlConfigSession<JavaMutableConfig> session = config.load();
        session.getValue().setPort(2);
        session.save();
        assertEquals(2, config.load().getValue().getPort());
    }

    @Test void listSerializerAndMoveKeepExistingPresentation() {
        JavaMemorySource source = new JavaMemorySource("- 'a' # a\n- b # b\n", true);
        YamlConfig<List<String>> config = YamlConfigs.create(engine, source, JavaApiSerializers.strings());
        YamlConfigSession<List<String>> session = config.load();
        session.setValue(List.of("b", "a"));
        session.save();
        assertEquals(session.getValue(), config.load().getValue());
        assertTrue(source.text.contains("'a' # a"), source.text);
        assertTrue(source.text.indexOf("# b") < source.text.indexOf("# a"), source.text);
    }

    @Test void failedWriteDoesNotPublishAndCanBeRetriedFromJava() {
        JavaMemorySource source = new JavaMemorySource("port: 25565\n", true);
        YamlConfig<JavaServerConfig> config = YamlConfigs.create(engine, source, JavaServerConfig.Companion.serializer());
        YamlConfigSession<JavaServerConfig> session = config.load();
        session.setValue(new JavaServerConfig("server", 30000, List.of()));
        source.failNextCommit = true;
        assertThrows(YamlWriteException.class, session::save);
        assertEquals("port: 25565\n", source.text);
        assertEquals(0, source.commits);
        session.save();
        assertEquals(session.getValue(), config.load().getValue());
        assertEquals(1, source.commits);
    }

    @Test void requiredFactoryArgumentsRejectNullBeforeIO() {
        JavaMemorySource source = new JavaMemorySource("", false);
        assertThrows(NullPointerException.class, () -> YamlConfigs.create(null, source, JavaServerConfig.Companion.serializer()));
        assertThrows(NullPointerException.class, () -> YamlConfigs.create(engine, null, JavaServerConfig.Companion.serializer()));
        assertThrows(NullPointerException.class, () -> YamlConfigs.create(engine, source, null));
        assertEquals(0, source.opens);
        assertEquals(0, source.writes);
    }
}
