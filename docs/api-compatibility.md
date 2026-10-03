# Public API / ABI checks

Core and snakeyaml use the binary compatibility validation built into the pinned
Kotlin Gradle plugin. It reads compiled Kotlin/JVM metadata; it is not a source
regex check. This DSL is experimental and must be retested when Kotlin changes.

Normal `build` / `check` also runs `checkKotlinAbi`. Reference dumps under each
module's `api/` are versioned. A changed or absent baseline fails the check.
Never run `updateKotlinAbi` automatically in normal verification just to pass CI.

Intentional API changes require reviewing the generated diff and compatibility
impact, then committing the accepted reference separately. Java consumer tests
also check useful source-level calls. ABI equality is not proof of unchanged
behavior, and it does not replace defaults/presentation/transaction tests.

For deliberate baseline maintenance only:

```shell
bash ci/generate-abi.sh
```

The `ABI_BOOTSTRAP=true` pipeline mode runs only dump generation. It is NOT a
successful full-build result; after committing reviewed dumps, run normal CI on
that new SHA. The baseline must contain generated declarations for both modules.

Source: https://kotlinlang.org/docs/gradle-binary-compatibility-validation.html
