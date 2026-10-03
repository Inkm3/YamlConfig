# YamlConfig

Kotlin/JVM・Javaから利用できる、kotlinx.serializationベースのYAML設定ライブラリです。
型付きの読み書き、YAML defaults、差分保存、既存コメントや表記の保持を提供します。
最低実行環境はJava 17です。

## 導入

JitPackを使用します。次は、所有者がGitHubをpublic化し、`v2.0.0`タグを公開・検証した後の依存指定です。
バージョンのプロパティを変更しただけでは、JitPackにそのバージョンが公開されるわけではありません。

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    implementation("com.github.Inkm3.YamlConfig:snakeyaml:v2.0.0")
}
```

`core`はEngine非依存のNode・codec・load/save・Editor SPI、`snakeyaml`はSnakeYAML Engine実装です。
通常は`snakeyaml`を使い、`core`は推移的依存として取得します。
Kotlinモデルをコンパイルするプロジェクトには、Kotlinと同じバージョンのserialization compiler pluginを適用してください。

## Kotlin

```kotlin
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.source.PathYamlSource
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class ServerConfig(
    val name: String = "server",
    val port: Int = 25565,
)

val config = yamlConfig<ServerConfig>(
    engine = SnakeYamlEngine(),
    userSource = PathYamlSource(Path.of("config.yml")),
)
val session = config.load()
session.value = session.value.copy(port = 30000)
session.save()
```

`val`中心のimmutableモデルを使用できます。`var`やmutable collectionの変更も保存できます。
同一sessionを複数threadから変更・保存する場合は外部同期が必要です。

## Java

上のKotlinモデルがコンパイル済みなら、Javaでは生成されたcompanion serializerを渡します。

```java
import com.github.inkm3.yamlconfig.YamlConfigs;
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine;
import com.github.inkm3.yamlconfig.source.PathYamlSource;
import java.nio.file.Path;

var config = YamlConfigs.create(
    new SnakeYamlEngine(),
    new PathYamlSource(Path.of("config.yml")),
    ServerConfig.Companion.serializer()
);
var session = config.load();
session.setValue(new ServerConfig(session.getValue().getName(), 30000));
session.save();
```

任意設定は`YamlConfigs.builder(engine, source, serializer)`を使用します。
Javaのrecord/POJOも明示的な`KSerializer`があれば利用できますが、自動reflection mappingは提供しません。
Kotlin pluginなしのJava-only consumerと、生成serializerを使用するKotlin consumerを配布JARに対して検証しています。

## 読み込みと保存

値の優先順位はuser YAML、YAML defaults、Kotlin constructor defaultsです。
objectの既知プロパティは再帰的に統合し、List/Map/inline/opaque contextual値は全体overrideとして扱います。
欠落と明示的な`null`、親のdefaultと明示的な空の子Mappingは区別します。

標準の`PRESERVE_OVERRIDES`は既存overrideを維持し、無変更時は書き込みません。
`MINIMAL_DIFFERENCE`は候補文書全体を再読み込みし、値を変えないoverrideだけを削除します。
これはgreedyな削減であり、大域的な最小ファイルを保証するものではありません。

保存は候補を検証し、forkしたEditorへ適用して実際のEditorも再検証し、write成功後だけsession状態を更新します。
`@EncodeDefault(NEVER)`等で必要な値を取得できず、正しい保存候補を作れない場合は書き込み前に失敗します。

## コメント・表記・identity

既存Nodeとの対応を確認できるList/Map/objectは構造編集し、コメント・未知キー・quote・数値やキーの表記を可能な範囲で保持します。
対応が不明なcustom serializer等では全体置換へ戻るため、すべての内部表記を保証するものではありません。

List内objectは、最初の必須・非inline scalarフィールドをidentity候補にできます。
変更前後で一意かつ非nullの場合に限りmoveと内部更新へ使います。必要なときだけ既存の`@YamlIdentity`／`@YamlIdentityDisabled`を使用します。
今回のJava対応に新しい独自annotationは不要です。

Anchor、Alias、Recursive Alias、Merge Key、Complex Mapping Keyは未対応です。
引用符で囲んだ文字列キー`"<<"`はmerge keyとは区別します。

## 検証

```bash
bash ./gradlew build
bash ./gradlew build -PtestJavaVersion=21
bash ./gradlew build -PtestJavaVersion=25
bash ci/verify-consumer.sh
```

公開ABIはバージョン管理した基準と比較します。通常CIでは基準を自動更新しません。
性能測定は`bash ./gradlew :benchmark:jmh`で別JVMを使用し、通常テストに時間の合否しきい値は設けません。
benchmarkは非公開モジュールで、利用者のruntime依存に入りません。

## ドキュメント

- [利用方法](docs/usage.md) / [Java API](docs/java-interop.md) / [移行ガイド](docs/migration-v2.md)
- [保存トランザクション](docs/transactional-save.md) / [省略値](docs/omitted-properties.md) / [Sequence identity](docs/sequence-identity.md)
- [ABI検査](docs/api-compatibility.md) / [JVM検証](docs/jvm-testing.md) / [JMH](docs/benchmarks.md)
- [配布consumer](samples/README.md) / [JitPack公開手順](docs/releasing.md) / [変更履歴](CHANGELOG.md)

## ライセンス

[Apache License 2.0](LICENSE)
