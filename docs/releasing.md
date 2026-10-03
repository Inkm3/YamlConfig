# Versioning and JitPack releases

Development uses `projectVersion=2.0.0-SNAPSHOT`. Keep that value while the planned
Java API and compatibility work are being verified. Changing the property is NOT
a release and does not create a Git tag. No tag or publication is created by CI.

## JitPack coordinates are Git refs

JitPack's version is the exact requested Git tag/commit/branch snapshot, not just
the `projectVersion` property. For a future tag named `v2.0.0`, use:

```kotlin
repositories { maven("https://jitpack.io") }
dependencies {
    implementation("com.github.Inkm3.YamlConfig:snakeyaml:v2.0.0")
}
```

The `v` is intentional: a tag called `v2.0.0` must not be documented as `2.0.0`.
Before a release, pin a verified GitHub commit for reproducibility, or explicitly
use the changing `dev-SNAPSHOT` ref. `2.0.0-SNAPSHOT` in Gradle does not by itself
create that version on JitPack.

## Release checklist

1. Finish and review the public API and its Kotlin/Java consumer tests.
2. Run the full build, compatibility checks and tests on supported JVMs.
3. Set `projectVersion=2.0.0` in a release-preparation branch; test that exact SHA.
4. Integrate and synchronize the exact commit to GitHub.
5. Create the immutable `v2.0.0` tag only at the final verified release SHA.
6. Build that tag on JitPack. Verify both core/snakeyaml POMs and artifacts from a
   separate consumer project, including transitive core/runtime dependencies.
7. Keep the release tag fixed. Start subsequent development with a new SNAPSHOT.

`jitpack.yml` selects Java 17 and keeps tests in the build command. It must not
silently use `-x test`. A successful `publishToMavenLocal` is packaging validation,
not proof that a JitPack-hosted artifact has already been built or downloaded.

## Private repositories

A private GitHub repository needs JitPack authorization and consumer access (or
an explicitly configured artifact-sharing policy). The owner will make this
repository public and synchronize it after the release work is complete. Do not
expose artifacts, purchase a plan, or store access tokens in Git as part of build
preparation.

## Sources

- https://docs.jitpack.io/building/ (multi-module coordinates and install command)
- https://docs.jitpack.io/private/ (private repository access)
- https://semver.org/ (version policy)
