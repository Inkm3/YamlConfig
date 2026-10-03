# Changelog

## 2.0.0 (release preparation)

This source version is prepared for release. Public GitHub synchronization,
visibility, tag creation and the external JitPack build are separate owner steps.

### Breaking changes from the old Schema-based API

- Removed `com.github.inkm3.yamlconfig.schema.*` and Schema-taking factories.
- `KSerializer<T>` / `@Serializable` is the supported typed conversion path.
- Immutable models and constructor defaults replace the former mutable Schema DSL.
- See docs/migration-v2.md for migration and default/null/collection semantics.

### Added and verified

- Java `YamlConfigs.create` / builder, source constructor overloads and static
  access to the default serialization format. Existing Kotlin factories remain.
- Java 17 bytecode, tested on Java 17/21/25 via explicit Test launchers.
- Compiled ABI baselines and a negative probe that proves ABI drift is rejected.
- Separate published-artifact Java and Kotlin consumers, using POM dependencies.
- JitPack Java 17 configuration, exact Git-tag coordinate documentation.
- Independent, non-published JMH module with forked workloads and allocation data.

### Existing serializer-based capabilities retained

- Whole-document-validated transactional save and failure retry.
- Object defaults overlay, atomic collection load, missing/null distinction.
- Structural List/Map updates, identity move/update, unknown-field ownership.
- Safe omitted-property preservation and failure before unrepresentable writes.
- Bounded validation memo and MINIMAL candidate construction improvements.

Performance candidates and their measured limitations are documented separately;
no general claim that all save operations become faster is made.
