# Changelog

## 2.0.0 (source release; external publication pending)

The source and artifact version is 2.0.0. GitHub synchronization, public visibility,
immutable tag creation and the external JitPack build are separate owner steps.
This entry does not claim that v2.0.0 can already be downloaded from JitPack.

### Breaking changes from the old Schema-based API

- Removed `com.github.inkm3.yamlconfig.schema.*` and Schema-taking factories.
- `KSerializer<T>` / `@Serializable` is the supported typed conversion path.
- Immutable models and constructor defaults replace the former mutable Schema DSL.
- See docs/migration-v2.md for migration and default/null/collection semantics.

### Java and quality

- Java `YamlConfigs.create` / builder, source constructor overloads and static
  access to the default serialization format. Existing Kotlin factories remain.
- Java 17 bytecode, tested on Java 17/21/25 via explicit Test launchers.
- Compiled ABI baselines and a negative probe that proves ABI drift is rejected.
- Separate published-artifact Java and Kotlin consumers, using POM dependencies.
- JitPack Java 17 configuration and exact Git-tag coordinate documentation.
- Independent, non-published JMH module with forked workloads and allocation data.
- Optional `FRESH_VERIFY=true` pipeline recompiles and reruns tests without build-cache reuse.

### Performance

- Large-Sequence exact/identity candidate indexing preserves original greedy edit
  ordering with collision checks. Lists below 128 elements retain the original diff.
- Plan-local bounded source-alignment memo reuses reference-identical checks across
  candidate retries without crossing saves or skipping actual-Editor validation.
- Reversed-order JMH pairs, raw score provenance and tradeoffs are documented in
  docs/sequence-performance.md. Large-case time point estimates improved with about
  1.2% additional allocation; smaller cases include regressions. No universal speedup
  or removal of the worst-case quadratic ArrayList movement is claimed.

### Existing serializer-based capabilities retained

- Whole-document-validated transactional save and failure retry.
- Object defaults overlay, atomic collection load, missing/null distinction.
- Structural List/Map updates, identity move/update, unknown-field ownership.
- Safe omitted-property preservation and failure before unrepresentable writes.
- Bounded validation memo and MINIMAL candidate construction improvements.

### Limits

Automatic Java POJO mapping is not added. Custom serializers must expose the values
to persist. Unsupported YAML structures and whole-replacement fallbacks retain
existing restrictions. Passing ABI checks is not a guarantee of all behavioral or
source compatibility. Public visibility, repository credentials and release tags
are not changed by build or CI scripts.
