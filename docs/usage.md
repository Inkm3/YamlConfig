# 使い方

## 基本

### 1. 設定クラスを作成する

```kotlin
data class ServerConfig(
    var name: String = "Server",
    var port: Int = 25565,
    var enabled: Boolean = true,
    var description: String? = null,
)
```

### 2. Schemaを定義する

```kotlin
import com.github.inkm3.yamlconfig.schema.boolean
import com.github.inkm3.yamlconfig.schema.int
import com.github.inkm3.yamlconfig.schema.nullable
import com.github.inkm3.yamlconfig.schema.str
import com.github.inkm3.yamlconfig.schema.yamlObject

val serverSchema = yamlObject(::ServerConfig) {
    field("name", ServerConfig::name, str())
    field("port", ServerConfig::port, int())
    field("enabled", ServerConfig::enabled, boolean())

    optionalField(
        "description",
        ServerConfig::description,
        nullable(str()),
    )
}
```

対応するYAML:

```yaml
name: "Lobby"
port: 25565
enabled: true
description: null
```

### 3. 設定を読み込む

```kotlin
import com.github.inkm3.yamlconfig.source.PathYamlSource
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.yamlConfig
import java.nio.file.Path

val config = yamlConfig(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(
        Path.of("config.yml"),
    ),
    schema = serverSchema,
)

val session = config.load()

println(session.value.name)
println(session.value.port)
```

`load()` は `YamlConfigSession` を返します。

読み込んだ設定値は、

```kotlin
session.value
```

から取得できます。

### 4. 値を変更して保存する

```kotlin
session.value.name = "Survival"
session.value.port = 25566

session.save()
```

`save()` を呼ぶと変更内容がYAMLへ保存されます。

---

## Optional field

フィールドそのものをYAMLから省略可能にする場合は
`optionalField(...)` を使用します。

```kotlin
optionalField(
    "description",
    ServerConfig::description,
    nullable(str()),
)
```

例えば、

```yaml
name: "Lobby"
port: 25565
enabled: true
```

のように `description` が存在しない場合、
`ServerConfig` のfactoryで設定された初期値がそのまま使用されます。

`nullable(...)` と `optionalField(...)` は意味が異なります。

```yaml
description: null
```

は「値としてnullが存在する」状態です。

一方、

```yaml
# description自体が存在しない
```

場合はOptional fieldとして扱われます。

---

## デフォルト設定

ユーザー設定とは別にデフォルトYAMLを指定できます。

例えば、resourcesに

```text
defaults.yml
```

を配置します。

```yaml
name: "Server"
port: 25565
enabled: true
```

読み込み時に、

```kotlin
import com.github.inkm3.yamlconfig.source.classpathYamlInput

val config = yamlConfig(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(
        Path.of("config.yml"),
    ),
    schema = serverSchema,
    defaultsSource =
        classpathYamlInput<ServerConfig>(
            "defaults.yml",
        ),
)
```

と指定できます。

ユーザー側の `config.yml` が、

```yaml
name: "Lobby"
```

だけの場合でも、

```text
name    = Lobby
port    = 25565
enabled = true
```

のように、不足している値はデフォルトYAMLから取得されます。

ユーザー設定が存在する値は、デフォルトより優先されます。

---

## 保存モード

標準では、

```kotlin
YamlSaveMode.PRESERVE_OVERRIDES
```

が使用されます。

明示的なユーザー設定をできるだけ残します。

ユーザー設定をデフォルトとの差分だけにしたい場合は、

```kotlin
val config = yamlConfig(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(
        Path.of("config.yml"),
    ),
    schema = serverSchema,
    defaultsSource =
        classpathYamlInput<ServerConfig>(
            "defaults.yml",
        ),
    saveMode =
        YamlSaveMode.MINIMAL_DIFFERENCE,
)
```

を使用できます。

`MINIMAL_DIFFERENCE` では、値がデフォルトと同じになった場合、
不要になったユーザー側のoverrideを削除します。

---

## List

```kotlin
data class Config(
    var servers: List<String> = emptyList(),
)

val schema = yamlObject(::Config) {
    field(
        "servers",
        Config::servers,
        list(str()),
    )
}
```

```yaml
servers:
  - "lobby"
  - "survival"
  - "creative"
```

---

## Map

```kotlin
data class Config(
    var ports: Map<String, Int> = emptyMap(),
)

val schema = yamlObject(::Config) {
    field(
        "ports",
        Config::ports,
        map(str(), int()),
    )
}
```

```yaml
ports:
  lobby: 25565
  survival: 25566
```

---

## ネストしたObject

```kotlin
data class DatabaseConfig(
    var host: String = "localhost",
    var port: Int = 3306,
)

data class AppConfig(
    var database: DatabaseConfig = DatabaseConfig(),
)

val schema = yamlObject(::AppConfig) {
    field(
        "database",
        AppConfig::database,
        obj(::DatabaseConfig) {
            field("host", DatabaseConfig::host, str())
            field("port", DatabaseConfig::port, int())
        },
    )
}
```

対応するYAML:

```yaml
database:
  host: "localhost"
  port: 3306
```