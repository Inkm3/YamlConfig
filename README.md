# YamlConfig

Kotlin向けの、スキーマベースでYAML設定を扱うライブラリです。

現在は開発中のため、APIやパッケージ構成が変更される可能性があります。

## 主な機能

- Kotlin向けのSchema DSL
- YAMLとKotlinオブジェクトの型付き変換
- `String` / `Boolean` / `Int` / `Long` / `Float` / `Double`
- Nullable / Optional field
- Object / List / Map
- 部分的なデフォルト設定
- `PRESERVE_OVERRIDES` / `MINIMAL_DIFFERENCE`
- Map / Listの構造的な保存
- 既存コメント・クォート・キー表現などを可能な範囲で保持
- SnakeYAML Engine実装

## モジュール

### `core`

YAMLエンジンに依存しないSchema、Node、Save、Editor SPIなどを含みます。

### `snakeyaml`

SnakeYAML Engineを利用した読み書きと、presentationを保持する編集処理を提供します。

通常はこちらを利用します。

## 導入

GitHubへ公開したバージョンをJitPackから利用します。

`repositories` にJitPackを追加します。

```kotlin
repositories {
    maven("https://jitpack.io")
}
```

SnakeYAML実装を利用する場合:

```kotlin
dependencies {
    implementation("com.github.inkm3.YamlConfig:snakeyaml:VERSION")
}
```

`core` のみ利用する場合:

```kotlin
dependencies {
    implementation("com.github.inkm3.YamlConfig:core:VERSION")
}
```

`VERSION` にはGitHubのTagを指定します。

例:

```kotlin
implementation("com.github.inkm3.YamlConfig:snakeyaml:v1.0.0")
```

## 基本例

```kotlin
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

詳しい使い方は [docs/usage.md](docs/usage.md) を参照してください。

## List / Map

```kotlin
data class Config(
    var servers: List<String> = emptyList(),
    var ports: Map<String, Int> = emptyMap(),
)

val schema = yamlObject(::Config) {
    field("servers", Config::servers, list(str()))
    field("ports", Config::ports, map(str(), int()))
}
```

## Nullable / Optional

`nullable(...)` は値としてのYAML `null` を許可します。

```kotlin
nullable(str())
```

フィールド自体を省略可能にする場合は `optionalField(...)` を使用します。

YAMLの `null` と「フィールドが存在しないこと」は別として扱われます。

## デフォルト設定

値は基本的に次の優先順位で解決されます。

```text
ユーザー設定
    ↓
YAMLデフォルト
    ↓
必要に応じてfactoryの初期値
```

List / Mapは、ユーザー側に存在する場合はCollection単位のoverrideとして扱います。

## 保存モード

### `PRESERVE_OVERRIDES`

ユーザーが明示したoverrideをできるだけ維持します。

### `MINIMAL_DIFFERENCE`

現在値がデフォルトと同じ場合はoverrideを削除し、ユーザー設定をできるだけ小さく保ちます。

## YAMLの制限

現在は次をサポートしていません。

- Anchor
- Alias
- Recursive Alias
- Merge Key
- Complex Mapping Key

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
