# YamlConfig

Kotlin向けの、スキーマベースでYAML設定を扱うライブラリです。

型安全なSchema DSLによるYAMLとKotlinオブジェクトの変換に加え、
デフォルト設定、差分保存、既存YAMLのpresentationを可能な範囲で維持した編集を提供します。

現在は開発中のため、APIやパッケージ構成が変更される可能性があります。

## 主な機能

- Kotlin向けのSchema DSL
- YAMLとKotlinオブジェクトの型付き変換
- `String` / `Boolean` / `Int` / `Long` / `Float` / `Double`
- Nullable / Optional field
- Object / List / Map
- 部分的なデフォルト設定
- 初回ロード時の設定ファイル生成
- `PRESERVE_OVERRIDES` / `MINIMAL_DIFFERENCE`
- Map / Listの構造的な保存
- 既存コメント・クォート・キー表現などを可能な範囲で保持
- SnakeYAML Engine実装

## モジュール

### `core`

YAMLエンジンに依存しないSchema、Node、Save、Editor SPIなどを含みます。

### `snakeyaml`

SnakeYAML Engineを利用した読み書きと、
presentationを保持する編集処理を提供します。

通常はこちらを利用します。

## 導入

JitPackを利用します。

```kotlin
repositories {
    maven("https://jitpack.io")
}
```

SnakeYAML実装を利用する場合:

```kotlin
dependencies {
    implementation("com.github.Inkm3.YamlConfig:snakeyaml:VERSION")
}
```

`core` のみ利用する場合:

```kotlin
dependencies {
    implementation("com.github.Inkm3.YamlConfig:core:VERSION")
}
```

`VERSION` にはGitHubのTagを指定します。

例:

```kotlin
implementation("com.github.Inkm3.YamlConfig:snakeyaml:v1.0.0")
```

## 基本例

```kotlin
import com.github.inkm3.yamlconfig.schema.boolean
import com.github.inkm3.yamlconfig.schema.int
import com.github.inkm3.yamlconfig.schema.nullable
import com.github.inkm3.yamlconfig.schema.str
import com.github.inkm3.yamlconfig.schema.yamlObject

data class ServerConfig(
    var name: String = "Server",
    var port: Int = 25565,
    var enabled: Boolean = true,
    var description: String? = null,
)

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

設定を読み込む場合:

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

println(session.value.name)

session.value.port = 25566
session.save()
```

詳しい使い方は [docs/usage.md](docs/usage.md) を参照してください。

## デフォルト設定

ユーザー設定とは別にデフォルトYAMLを指定できます。

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

値は基本的に次の優先順位で解決されます。

```text
ユーザー設定
    ↓
YAMLデフォルト
    ↓
必要に応じてfactoryの初期値
```

`defaultsSource` を指定していてユーザー設定ファイルが存在しない場合、
標準ではdefaultsの内容をそのままコピーして設定ファイルを生成します。

この動作は `YamlMissingFilePolicy` で変更できます。

詳しくは [設定ファイルが存在しない場合](docs/usage.md#設定ファイルが存在しない場合)
を参照してください。

## 保存モード

標準では `PRESERVE_OVERRIDES` が使用され、
ユーザーが明示したoverrideをできるだけ維持します。

`MINIMAL_DIFFERENCE` を使用すると、
defaultsと同じ値になったoverrideを削除し、
ユーザー設定をdefaultsとの差分に近い状態で保存できます。

詳しくは [保存モード](docs/usage.md#保存モード) を参照してください。

## YAMLの制限

現在はAnchor、Alias、Recursive Alias、Merge Key、
Complex Mapping Keyをサポートしていません。

これらはエラーとして扱います。

Quoted keyは通常の文字列として扱われます。

```yaml
"<<": "value"
```

## テスト

```bash
./gradlew test
```

## ビルド

```bash
./gradlew build
```

## ライセンス

Apache License 2.0

詳細は [LICENSE](LICENSE) を参照してください。
