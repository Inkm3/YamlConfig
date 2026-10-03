# 旧 Schema API から kotlinx.serialization への移行

このブランチでは旧 `YamlSchema` / Schema DSL を削除し、
`kotlinx.serialization` を公開 Config API の唯一の型変換経路にしました。

## 旧形式

```kotlin
data class Config(
    var port: Int = 25565,
)

val schema = yamlObject(::Config) {
    field("port", Config::port, int())
}

val config = yamlConfig(
    engine = engine,
    userSource = source,
    schema = schema,
)
```

## 新形式

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val port: Int = 25565,
)

val config = yamlConfig<Config>(
    engine = engine,
    userSource = source,
)
```

更新は次のように行えます。

```kotlin
val session = config.load()
session.value = session.value.copy(port = 30000)
session.save()
```

## field 名

旧 DSL の field name は `@SerialName` へ置き換えます。

```kotlin
@Serializable
data class Config(
    @SerialName("server-port")
    val port: Int = 25565,
)
```

## optional / nullable

旧 `optionalField` は serializer の constructor default / optional property で表現します。

```kotlin
@Serializable
data class Config(
    val message: String = "default",
    val description: String? = null,
)
```

missing と YAML `null` は別です。
required nullable が必要なら default を付けず `val description: String?` とします。

## List / Map

旧 `list(...)` / `map(...)` は Kotlin の型そのものを使います。

```kotlin
@Serializable
data class Config(
    val servers: List<Server> = emptyList(),
    val ports: Map<String, Int> = emptyMap(),
)
```

## custom 型

Schema を追加する代わりに `KSerializer<T>` または contextual serializer を利用します。

```kotlin
val config = yamlConfig(
    engine = engine,
    userSource = source,
    serializer = MyConfigSerializer,
)
```

reified overloadへ `YamlSerialization(serializersModule = ...)` を渡す方法もあります。

## default の違い

新経路では優先順位を次のように扱います。

```text
user YAML
  ↓
YAML defaults
  ↓
Kotlin serialization constructor defaults
```

nested object の parent property が missing の場合と、明示的 `{}` の場合は異なります。
List / Map は load では collection 全体の override です。

## breaking change

旧 `com.github.inkm3.yamlconfig.schema.*`、
`YamlSchema`、Schema を受け取る `yamlConfig(...)` overload は削除されています。

この変更は source-compatible ではありません。旧 DSL を利用していたコードは
`@Serializable` model または明示的 serializer へ移行してください。
