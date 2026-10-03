# Published-artifact consumers

These are separate Gradle builds, NOT included projects or composite builds.
`ci/verify-consumer.sh` publishes core/snakeyaml to an isolated file repository,
then compiles and runs both consumers using ONLY the snakeyaml Maven dependency.
Own artifacts are resolved exclusively from that repository. Both readers disable
Gradle module metadata redirection so the POM and its transitive dependencies are
actually exercised. No stale mavenLocal fallback is allowed.

- java-consumer: application plugin only; no Kotlin compiler plugin. A Java record
  uses an explicit KSerializer, then performs actual file load/save/reload.
- kotlin-consumer: pinned Kotlin/compiler serialization plugins generate the model
  serializer; defaults, SerialName, immutable copy, unknown keys, comments and no-op
  save are checked from published artifacts.

Update the sample Kotlin plugin versions with gradle/libs.versions.toml during a
future Kotlin upgrade. These tests do not prove an external JitPack URL is live.
