# Java consumers

The Java API uses the same `KSerializer<T>`, defaults, candidate validation,
structural edits and transactional save path as the Kotlin factory. It adds no
YamlConfig-specific annotation and does not automatically map arbitrary POJOs.
The Kotlin top-level functions (and their JVM facade) remain available unchanged.

For a Kotlin model compiled with the serialization plugin:

```kotlin
@kotlinx.serialization.Serializable
data class ServerConfig(val name: String = "server", val port: Int = 25565)
```

Java can use its generated companion serializer:

```java
import com.github.inkm3.yamlconfig.YamlConfig;
import com.github.inkm3.yamlconfig.YamlConfigSession;
import com.github.inkm3.yamlconfig.YamlConfigs;
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine;
import com.github.inkm3.yamlconfig.source.PathYamlSource;
import java.nio.file.Path;

YamlConfig<ServerConfig> config = YamlConfigs.create(
    new SnakeYamlEngine(),
    new PathYamlSource(Path.of("config.yml")),
    ServerConfig.Companion.serializer()
);
YamlConfigSession<ServerConfig> session = config.load();
ServerConfig previous = session.getValue();
session.setValue(new ServerConfig(previous.getName(), 30000));
session.save();
```

`ServerConfig.serializer()` is Kotlin syntax: without a separate static bridge,
Java calls `ServerConfig.Companion.serializer()`. Java also supplies all arguments
to an ordinary Kotlin data-class constructor unless that model has Java overloads.

For optional settings:

```java
import com.github.inkm3.yamlconfig.save.YamlSaveMode;
import com.github.inkm3.yamlconfig.source.ClasspathYamlInput;
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy;

YamlConfig<ServerConfig> config = YamlConfigs.builder(
        new SnakeYamlEngine(),
        new PathYamlSource(Path.of("config.yml")),
        ServerConfig.Companion.serializer())
    .defaultsSource(new ClasspathYamlInput(
        ServerConfig.class.getClassLoader(), "defaults.yml"))
    .missingFilePolicy(YamlMissingFilePolicy.DO_NOT_CREATE)
    .saveMode(YamlSaveMode.MINIMAL_DIFFERENCE)
    .build();
```

The automatic missing-file policy is determined when `build()` is called: defaults
present -> COPY_DEFAULTS, absent -> DO_NOT_CREATE. An explicit policy is retained
if defaults are subsequently changed. `automaticMissingFilePolicy()` resets it.
`defaultsSource(null)` removes defaults. Built configs snapshot these options and
are not changed by later builder calls. Sources and serializers are not deep-copied.
No I/O occurs until `load()` (or an explicit later save).

`PathYamlSource(Path)` and `ClasspathYamlInput(ClassLoader, String)` default to
UTF-8. Their existing full constructors still exist. `YamlSerialization.getDefault()`
is available from Java; the existing companion getter is retained. Explicit codec
methods still require a serializer. No reified overload is advertised to Java.

A Java record/POJO can be used with an explicitly supplied compatible serializer;
there is no automatic `create(SomePojo.class, ...)` or reflection-based defaults.
A Java-only caller does not need the Kotlin compiler plugin just to call a library
or consume an already compiled serializer, but Kotlin models do need it where
those models are compiled. Kotlin/serialization runtime dependencies remain.

Builders and sessions require external synchronization. Existing failure and
presentation limitations are unchanged; Java convenience is not a new save engine.

Reference: https://kotlinlang.org/docs/java-to-kotlin-interop.html
