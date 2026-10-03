# YamlConfig

Kotlin の `kotlinx.serialization` を使って YAML 設定を型付きで読み書きするライブラリです。

`@Serializable` な設定クラスをそのまま利用でき、YAML defaults、差分保存、
既存 YAML のコメント・クォート・キー表現などを可能な範囲で維持した編集を提供します。

現在は開発中のため、API やパッケージ構成が変更される可能性があります。

## 主な機能

- `kotlinx.serialization` / `KSerializer<T>` ベースの型付き Config
- immutable な `data class` と `val` を利用可能
- constructor default / nullable / enum / `@SerialName`
- List / Map / nested object
- contextual / custom serializer
- YAML defaults と user YAML の overlay
- `PRESERVE_OVERRIDES` / `MINIMAL_DIFFERENCE`
- List / Map の構造保存と Sequence identity
- 既存コメント・クォート・scalar/key 表現を可能な範囲で保持
- 保存候補の全体再検証と失敗時 rollback 境界
- SnakeYAML Engine 実装

## モジュール

### `core`

YAML Engine に依存しない Node、serialization codec、load/save、Editor SPI を含みます。

### `snakeyaml`

SnakeYAML Engine を利用した読み書きと presentation-preserving editing を提供します。
通常はこちらを利用します。

## 導入

JitPack を利用します。

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Inkm3.YamlConfig:snakeyaml:VERSION")
}
```

`core` のみ利用する場合:

```kotlin
implementation("com.github.Inkm3.YamlConfig:core:VERSION")
```

利用側プロジェクトでは Kotlin serialization plugin も有効にしてください。

## 基本例

```kotlin
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.source.PathYamlSource
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class ServerConfig(
    val name: String = "Server",
    val port: Int = 25565,
    val enabled: Boolean = true,
    val description: String? = null,
)

val config = yamlConfig<ServerConfig>(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
)

val session = config.load()

println(session.value.port)

session.value = session.value.copy(port = 25566)
session.save()
```

`var` や mutable collection も利用できますが、immutable な値を `copy` で更新する方法を推奨します。

## YAML defaults

```kotlin
import com.github.inkm3.yamlconfig.source.classpathYamlInput

val config = yamlConfig<ServerConfig>(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
    defaultsSource = classpathYamlInput<ServerConfig>("defaults.yml"),
)
```

値は概ね次の優先順位で決まります。

```text
user YAML
  ↓
YAML defaults
  ↓
Kotlin constructor defaults
```

object は既知 property 単位で overlay します。
List / Map / inline / opaque contextual value は whole-value override です。
missing と明示的 YAML `null` は区別します。

## 保存モード

標準は `YamlSaveMode.PRESERVE_OVERRIDES` です。

`MINIMAL_DIFFERENCE` は、現在値を変えないことを文書全体で検証しながら
不要な user override を greedily 削除します。大域的な最小 YAML を保証するものではありません。

## Presentation

SnakeYAML 実装では、変更していない既存 Node をできるだけ再利用します。
List の move や Map の surviving key、object 内の既知 property 更新では、
コメント・quote・raw scalar/key 表現を可能な範囲で保持します。

source と serializer representation の対応を安全に確認できない場合は whole-value replacement に戻るため、
任意の custom serializer で subtree presentation の完全保持を保証するものではありません。

## Sequence identity

List 内 object の move + update では、最初の required non-inline scalar property を
暗黙 identity 候補として使います。一意かつ non-null である場合だけ identity match します。

必要な場合のみ `@YamlIdentity` / `@YamlIdentityDisabled` で明示できます。
required であること自体は業務上の stable identity を保証しません。

## YAML の制限

現在は Anchor、Alias、Recursive Alias、Merge Key、Complex Mapping Key をサポートしていません。
Quoted `"<<“` ではなく、通常の文字列キー `"<<“` は merge key と区別されます。

## ドキュメント

- [使い方](docs/usage.md)
- [旧 Schema API からの移行](docs/migration-v2.md)
- [transactional save](docs/transactional-save.md)
- [Sequence identity](docs/sequence-identity.md)
- [省略 property の保存](docs/omitted-properties.md)

## テスト / ビルド

```bash
./gradlew test
./gradlew build
```

性能診断は通常テストから分離されています。

```bash
./gradlew :core:test --tests '*ProfileTest' -PprofileSavePlanner=true
```

## ライセンス

Apache License 2.0。詳細は [LICENSE](LICENSE) を参照してください。
