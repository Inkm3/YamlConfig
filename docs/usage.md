# YamlConfig の使い方

## 1. Config を定義する

`kotlinx.serialization` の `@Serializable` を使います。

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    @SerialName("server-name")
    val name: String = "Server",
    val port: Int = 25565,
    val enabled: Boolean = true,
    val description: String? = null,
)
```

Schema DSL は不要です。constructor default が Kotlin 側の default として使われます。

## 2. 読み込む

```kotlin
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.source.PathYamlSource
import com.github.inkm3.yamlconfig.yamlConfig
import java.nio.file.Path

val config = yamlConfig<ServerConfig>(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
)

val session = config.load()
println(session.value)
```

明示的な serializer を使う場合:

```kotlin
val config = yamlConfig(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
    serializer = ServerConfig.serializer(),
)
```

reified overload は指定した `YamlSerialization.serializersModule` を使って serializer を解決するため、
contextual serializer も利用できます。

## 3. 更新して保存する

immutable Config では `copy` で更新します。

```kotlin
session.value = session.value.copy(port = 25566)
session.save()
```

`var` や mutable collection の in-place 更新も検出できますが、共有状態を減らすため immutable value を推奨します。

`save()` は外部同期を行いません。同一 session を複数 thread から同時に変更・保存しないでください。

## defaults

```kotlin
import com.github.inkm3.yamlconfig.source.classpathYamlInput

val config = yamlConfig<ServerConfig>(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
    defaultsSource = classpathYamlInput<ServerConfig>("defaults.yml"),
)
```

object の既知 property は再帰 overlay します。

```yaml
# defaults.yml
database:
  host: production
  port: 3306
```

```yaml
# config.yml
database:
  port: 5432
```

なら、object serializer 上では `host=production, port=5432` になります。

一方 List / Map は user 側に値があれば collection 全体を override します。
明示的な空 List / Map も override です。

### missing と null

property が user YAML に存在しない場合は defaults、さらに serializer の constructor default が使われます。

```yaml
description: null
```

は明示的 null であり、missing とは異なります。

親 property 自体が missing で constructor default を持つ場合と、
親が `{}` として存在して child serializer の defaults を使う場合も区別します。

## 設定ファイルが存在しない場合

`YamlMissingFilePolicy` は `YamlSource.exists() == false` の場合だけ適用されます。
既存の空ファイルは missing ではありません。

- `DO_NOT_CREATE`: file を作らず load を続ける
- `CREATE_EMPTY`: 空 file を作る
- `COPY_DEFAULTS`: defaults YAML の元テキストをそのままコピーする

省略時は、defaults があれば `COPY_DEFAULTS`、なければ `DO_NOT_CREATE` です。

```kotlin
val config = yamlConfig<ServerConfig>(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
    defaultsSource = classpathYamlInput<ServerConfig>("defaults.yml"),
    missingFilePolicy = YamlMissingFilePolicy.DO_NOT_CREATE,
)
```

`COPY_DEFAULTS` は parse/re-encode せず raw text をコピーするので、
defaults ファイルのコメント・空行・quote を維持します。

## 保存モード

### PRESERVE_OVERRIDES

標準モードです。user YAML に明示された override をできるだけ維持します。
現在値に変更がなければ fork / write を行いません。

### MINIMAL_DIFFERENCE

```kotlin
val config = yamlConfig<ServerConfig>(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
    defaultsSource = classpathYamlInput<ServerConfig>("defaults.yml"),
    saveMode = YamlSaveMode.MINIMAL_DIFFERENCE,
)
```

削除候補を反映した文書全体を同じ defaults で再 decode / encode し、
現在値の serializer representation と一致するときだけ override を削除します。

これは greedy reduction であり、大域的な最小表現を求めるものではありません。
未知 object entry を含む親は安全のため無条件に削除しません。

## List

```kotlin
@Serializable
data class Config(
    val servers: List<String> = emptyList(),
)
```

load 時は whole-value override です。
save 時は、source alignment を確認できる既存 List について insert / remove / move / update を構造編集し、
変更していない element の presentation をできるだけ維持します。

### object identity

```kotlin
@Serializable
data class Server(
    val name: String,
    val port: Int = 25565,
)
```

この例では最初の required non-inline scalar である `name` が暗黙 identity 候補です。
変更前後の両 List で一意かつ non-null の場合、move + nested update が可能です。

自動規約が適切でない場合のみ:

```kotlin
@Serializable
data class Entry(
    val label: String,
    @YamlIdentity val id: Int = 0,
)
```

または class に `@YamlIdentityDisabled` を指定できます。

## Map

```kotlin
@Serializable
data class Config(
    val ports: Map<String, Int> = emptyMap(),
)
```

load 時は whole-value override です。
save 時は正規化した scalar key が一致する既存 entry を再利用し、
`0xff` や `TRUE` などの元の key lexeme を可能な範囲で維持します。

key rename は remove + insert として扱い、削除した entry の未知情報を別 entry へ移しません。

## encode default と省略 property

保存検証には `encodeDefaults = true` の serializer representation を使いますが、
`@EncodeDefault(NEVER)` など serializer 自身が省略する property は完全な semantic snapshot になりません。

既存 raw override を保持して現在値と一致するか検証し、安全に表現できない場合は write 前に失敗します。
必要な値を必ず encode する設計なら、標準の `@EncodeDefault(ALWAYS)` または custom serializer を検討してください。

## custom / contextual serializer

明示 `KSerializer<T>` と `SerializersModule` を利用できます。
ただし transforming serializer では descriptor と source representation の対応が確認できない場合があり、
その subtree は whole replacement へ fallback します。

serializer / constructor default は deterministic であることを前提とします。
deserialize 呼び出し回数や transient/unencoded state は保存契約に含みません。

## 保存の transaction

保存は概ね次の順序です。

```text
current value を encode
  ↓
候補 user document を構築
  ↓
defaults 込みで全体 decode / encode 検証
  ↓
Editor を fork
  ↓
構造編集を適用
  ↓
実 Editor を再検証
  ↓
write
  ↓
成功後だけ Editor / baseline を更新
```

prepare / edit / validation / write が失敗した場合、session は前回成功した baseline を維持します。
外部 bytes 自体の rollback は `YamlEngine` / `YamlOutput` 実装の契約です。

## YAML の制限

現在未対応:

- Anchor
- Alias
- Recursive Alias
- Merge Key
- Complex Mapping Key

quoted `"<<“` は通常文字列キーとして扱われます。

## Presentation の保持

SnakeYAML Engine は既存 native Node を可能な範囲で再利用します。
ただし whole-value replacement が必要な subtree や任意 transforming serializer では
内部コメント・style の完全保持を保証しません。
