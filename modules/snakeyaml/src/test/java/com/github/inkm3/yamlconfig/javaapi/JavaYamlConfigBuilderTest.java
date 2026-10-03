package com.github.inkm3.yamlconfig.javaapi;

import com.github.inkm3.yamlconfig.YamlConfig;
import com.github.inkm3.yamlconfig.YamlConfigBuilder;
import com.github.inkm3.yamlconfig.YamlConfigs;
import com.github.inkm3.yamlconfig.exception.YamlConfigException;
import com.github.inkm3.yamlconfig.save.YamlSaveMode;
import com.github.inkm3.yamlconfig.serialization.YamlSerialization;
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine;
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy;
import java.util.List;
import javaapi.JavaApiSerializers;
import javaapi.JavaContextualConfig;
import javaapi.JavaEndpoint;
import javaapi.JavaServerConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JavaYamlConfigBuilderTest {
    private final SnakeYamlEngine engine = new SnakeYamlEngine();
    private YamlConfigBuilder<JavaServerConfig> builder(JavaMemorySource source) {
        return YamlConfigs.builder(engine, source, JavaServerConfig.Companion.serializer());
    }
    private JavaMemorySource defaults() {
        return new JavaMemorySource("# defaults\nport: 30000\n", true);
    }

    @Test void creatingAndConfiguringTheBuilderDoesNotPerformIO() {
        JavaMemorySource source = new JavaMemorySource("", false);
        JavaMemorySource defaults = defaults();
        builder(source).defaultsSource(defaults).saveMode(YamlSaveMode.MINIMAL_DIFFERENCE).build();
        assertEquals(0, source.opens + source.writes + defaults.opens);
    }

    @Test void automaticPolicyUsesTheFinalDefaultsAndCopiesVerbatim() {
        JavaMemorySource source = new JavaMemorySource("", false);
        JavaMemorySource defaults = defaults();
        YamlConfig<JavaServerConfig> config = builder(source).defaultsSource(defaults).build();
        assertEquals(30000, config.load().getValue().getPort());
        assertEquals(defaults.text, source.text);
        assertTrue(source.present);
    }

    @Test void removingDefaultsRestoresAutomaticDoNotCreate() {
        JavaMemorySource source = new JavaMemorySource("", false);
        YamlConfig<JavaServerConfig> config = builder(source).defaultsSource(defaults()).defaultsSource(null).build();
        assertEquals(25565, config.load().getValue().getPort());
        assertFalse(source.present);
        assertEquals(0, source.writes);
    }

    @Test void explicitPolicyIsIndependentOfSetterOrder() {
        for (boolean policyFirst : List.of(false, true)) {
            JavaMemorySource source = new JavaMemorySource("", false);
            YamlConfigBuilder<JavaServerConfig> b = builder(source);
            if (policyFirst) b.missingFilePolicy(YamlMissingFilePolicy.DO_NOT_CREATE);
            b.defaultsSource(defaults());
            if (!policyFirst) b.missingFilePolicy(YamlMissingFilePolicy.DO_NOT_CREATE);
            assertEquals(30000, b.build().load().getValue().getPort());
            assertFalse(source.present);
        }
    }

    @Test void explicitCreateEmptySurvivesRemovingDefaults() {
        JavaMemorySource source = new JavaMemorySource("", false);
        builder(source).missingFilePolicy(YamlMissingFilePolicy.CREATE_EMPTY)
            .defaultsSource(defaults()).defaultsSource(null).build().load();
        assertTrue(source.present);
        assertEquals("", source.text);
    }

    @Test void automaticPolicyCanBeRestoredAfterAnExplicitChoice() {
        JavaMemorySource source = new JavaMemorySource("", false);
        builder(source).missingFilePolicy(YamlMissingFilePolicy.COPY_DEFAULTS)
            .automaticMissingFilePolicy().build().load();
        assertFalse(source.present);
    }

    @Test void invalidExplicitPolicyStillUsesTheExistingFailureContract() {
        JavaMemorySource source = new JavaMemorySource("", false);
        assertThrows(YamlConfigException.class, () -> builder(source)
            .missingFilePolicy(YamlMissingFilePolicy.COPY_DEFAULTS).build().load());
        assertFalse(source.present);
    }

    @Test void builtConfigsSnapshotOptionsDespiteLaterBuilderMutation() {
        JavaMemorySource source = new JavaMemorySource("", true);
        YamlConfigBuilder<JavaServerConfig> b = builder(source);
        YamlConfig<JavaServerConfig> first = b.build();
        YamlConfig<JavaServerConfig> second = b.defaultsSource(defaults()).build();
        assertEquals(25565, first.load().getValue().getPort());
        assertEquals(30000, second.load().getValue().getPort());
    }

    @Test void minimalModeIsNotLostAtTheJavaBoundary() {
        JavaMemorySource source = new JavaMemorySource("port: 25565\n", true);
        YamlConfig<JavaServerConfig> config = builder(source).saveMode(YamlSaveMode.MINIMAL_DIFFERENCE).build();
        config.load().save();
        assertEquals(25565, config.load().getValue().getPort());
        assertFalse(source.text.contains("port:"), source.text);
    }

    @Test void contextualModuleAndOptionsReachTheExistingLoaderAndSaver() {
        JavaMemorySource source = new JavaMemorySource("endpoint: old\nfuture: keep\n", true);
        YamlSerialization format = JavaApiSerializers.contextualFormat();
        YamlConfig<JavaContextualConfig> config = YamlConfigs.builder(
            engine, source, JavaContextualConfig.Companion.serializer()).serialization(format).build();
        var session = config.load();
        assertEquals("old", session.getValue().getEndpoint().getValue());
        session.setValue(new JavaContextualConfig(new JavaEndpoint("new")));
        session.save();
        assertEquals("new", config.load().getValue().getEndpoint().getValue());
        assertTrue(source.text.contains("future: keep"));
        assertFalse(format.getIgnoreUnknownKeys());
        assertFalse(format.getEncodeDefaults());
    }
}
