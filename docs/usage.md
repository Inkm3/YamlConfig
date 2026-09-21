# YamlConfigの使い方

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
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.source.PathYamlSource
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
```

`load()` は `YamlConfigSession` を返します。

読み込んだ値は `session.value` から取得できます。

```kotlin
println(session.value.name)
println(session.value.port)
```

### 4. 値を変更して保存する

```kotlin
session.value.name = "Survival"
session.value.port = 25566

session.save()
```

`save()` を呼ぶと変更内容がユーザーYAMLへ保存されます。

---

## Optional fieldとNullable

`nullable(...)` は、YAML上に値が存在し、その値が `null` であることを許可します。

```kotlin
nullable(str())
```

例えば、

```yaml
description: null
```

は明示的なYAML `null` です。

一方、フィールド自体をYAMLから省略可能にする場合は `optionalField(...)` を使用します。

```kotlin
optionalField(
    "description",
    ServerConfig::description,
    nullable(str()),
)
```

`description` 自体が存在しない場合、factoryで設定された初期値がそのまま使用されます。

したがって `description: null` と「`description` が存在しない」は別の状態として扱われます。

---

## デフォルト設定

ユーザー設定とは別にデフォルトYAMLを指定できます。

例えば `src/main/resources/defaults.yml` に次の内容を配置します。

```yaml
name: "Server"
port: 25565
enabled: true
```

`classpathYamlInput(...)` を使用して指定できます。

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

ユーザー側の `config.yml` に、

```yaml
name: "Lobby"
```

だけが存在する場合でも、実効値は次のようになります。

```text
name    = Lobby
port    = 25565
enabled = true
```

ユーザー設定が存在する値はdefaultsより優先されます。

```text
ユーザー設定
    ↓
YAMLデフォルト
    ↓
必要に応じてfactoryの初期値
```

---

## 設定ファイルが存在しない場合

`YamlMissingFilePolicy` を使用すると、`load()` 時にユーザー設定が存在しない場合の動作を指定できます。

Policyは `YamlSource.exists()` が `false` を返した場合だけ適用されます。
既に存在する空ファイルはMissingとは扱われません。

### デフォルトの動作

`missingFilePolicy` を省略した場合、`defaultsSource` の有無によってPolicyが自動的に決まります。

```text
defaultsSourceあり
    → COPY_DEFAULTS

defaultsSourceなし
    → DO_NOT_CREATE
```

そのため通常は、defaultsを指定するだけで `config.yml` が存在しない場合に `defaults.yml` の内容がコピーされます。

### `DO_NOT_CREATE`

ユーザー設定が存在していなくてもファイルを生成しません。

```kotlin
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy

val config = yamlConfig(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
    schema = serverSchema,
    defaultsSource = classpathYamlInput<ServerConfig>("defaults.yml"),
    missingFilePolicy = YamlMissingFilePolicy.DO_NOT_CREATE,
)
```

`config.yml` が存在しなくても、値自体はdefaultsから読み込めます。

### `CREATE_EMPTY`

空のユーザー設定を生成してから読み込みます。

```kotlin
val config = yamlConfig(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
    schema = serverSchema,
    defaultsSource = classpathYamlInput<ServerConfig>("defaults.yml"),
    missingFilePolicy = YamlMissingFilePolicy.CREATE_EMPTY,
)
```

生成される `config.yml` は空ですが、不足している値はdefaultsから取得されます。

### `COPY_DEFAULTS`

defaultsの内容をユーザー設定へそのままコピーします。

```kotlin
val config = yamlConfig(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
    schema = serverSchema,
    defaultsSource = classpathYamlInput<ServerConfig>("defaults.yml"),
    missingFilePolicy = YamlMissingFilePolicy.COPY_DEFAULTS,
)
```

例えば `defaults.yml` が、

```yaml
# Server configuration

# Server name
name: "Server"

# Listening port
port: 25565

enabled: true
```

であれば、初回ロード時に同じ内容の `config.yml` が生成されます。

`COPY_DEFAULTS` はdefaultsを解析して再シリアライズするのではなく、元のYAMLテキストをそのままコピーします。
そのため、コメント、空行、クォートなどのpresentationも維持されます。

### 既存ファイルは上書きしない

Policyはユーザー設定が存在しない場合だけ適用されます。

`COPY_DEFAULTS` を指定していても、既存の `config.yml` は上書きされません。
空の `config.yml` が既に存在する場合も同様です。

```text
Missing file
    → Policyを適用

Existing empty file
    → Policyを適用しない
```

### `COPY_DEFAULTS` とdefaults

`COPY_DEFAULTS` が実際に適用される場合は `defaultsSource` が必要です。

ユーザー設定が存在せず、`COPY_DEFAULTS` を指定しているにもかかわらず
`defaultsSource` が設定されていない場合はエラーになります。

既にユーザー設定が存在している場合はコピー処理自体が不要なため、defaultsは要求されません。

---

## 保存モード

標準では `YamlSaveMode.PRESERVE_OVERRIDES` が使用されます。

### `PRESERVE_OVERRIDES`

ユーザーが明示的に持っているoverrideをできるだけ維持します。

### `MINIMAL_DIFFERENCE`

ユーザー設定をdefaultsとの差分に近い状態で保存します。

```kotlin
import com.github.inkm3.yamlconfig.save.YamlSaveMode

val config = yamlConfig(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
    schema = serverSchema,
    defaultsSource = classpathYamlInput<ServerConfig>("defaults.yml"),
    saveMode = YamlSaveMode.MINIMAL_DIFFERENCE,
)
```

現在値がdefaultsと同じになった場合、不要になったユーザー側のoverrideを削除します。

これにより、後からdefaults側の値が変更された場合に新しいdefaultを継承できるようになります。

### `COPY_DEFAULTS` との組み合わせ

`COPY_DEFAULTS` で生成された内容は、通常のユーザー設定として扱われます。

`PRESERVE_OVERRIDES` ではコピーされた値は基本的にそのまま維持されます。

一方、`MINIMAL_DIFFERENCE` ではdefaultsと同じ値が不要なoverrideとして削除される場合があります。

```text
COPY_DEFAULTS
    ↓
完全なconfig.ymlを生成
    ↓
MINIMAL_DIFFERENCEでsave()
    ↓
defaultsと同じoverrideが削除される場合がある
```

完全な設定ファイルを利用者に見せ続けたい場合は、標準の `PRESERVE_OVERRIDES` が適しています。

---

## List

```kotlin
import com.github.inkm3.yamlconfig.schema.list

data class Config(
    var servers: List<String> = emptyList(),
)

val schema = yamlObject(::Config) {
    field("servers", Config::servers, list(str()))
}
```

```yaml
servers:
  - "lobby"
  - "survival"
  - "creative"
```

Listはユーザー側に存在する場合、Collection全体がユーザー側のoverrideとして扱われます。

---

## Map

```kotlin
import com.github.inkm3.yamlconfig.schema.map

data class Config(
    var ports: Map<String, Int> = emptyMap(),
)

val schema = yamlObject(::Config) {
    field("ports", Config::ports, map(str(), int()))
}
```

```yaml
ports:
  lobby: 25565
  survival: 25566
```

MapもListと同様に、ユーザー側に存在する場合はCollection単位のoverrideとして扱われます。

---

## ネストしたObject

```kotlin
import com.github.inkm3.yamlconfig.schema.obj

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

```yaml
database:
  host: "localhost"
  port: 3306
```

---

## YAMLの制限

現在は次のYAML機能をサポートしていません。

- Anchor
- Alias
- Recursive Alias
- Merge Key
- Complex Mapping Key

これらを読み込んだ場合はエラーになります。

クォートされた `"<<"` は通常の文字列キーとして扱われます。

```yaml
"<<": "value"
```

---

## Presentationの保持

SnakeYAML Engine実装では、既存YAMLを保存する際にpresentationを可能な範囲で維持します。

例えば変更されていない既存値については、

```yaml
name: 'Lobby'
```

のクォート表現やコメントなどを可能な範囲で保持します。

Listの並び替えやMapの更新についても、可能な限り既存Nodeを利用して編集します。

ただし、YamlConfigはYAMLのpresentationを完全に保持することを保証するものではありません。
