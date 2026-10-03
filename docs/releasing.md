# Versioning and JitPack release

The prepared source version is `projectVersion=2.0.0`. Development before this
milestone used `2.0.0-SNAPSHOT`. A version property is not a publication and does
not create a tag. CI does not change visibility or create releases/tags. The
owner performs final GitHub synchronization/publication after GitLab verification.

## Exact coordinates

JitPack resolves the exact Git tag or commit, not just projectVersion. For the
future immutable `v2.0.0` tag, consumers use:

```kotlin
repositories { maven("https://jitpack.io") }
dependencies {
    implementation("com.github.Inkm3.YamlConfig:snakeyaml:v2.0.0")
}
```

`core` can be used as `com.github.Inkm3.YamlConfig:core:v2.0.0` without SnakeYAML.
A tag named `v2.0.0` must not be documented with dependency version `2.0.0`.
Branch versions such as `dev-SNAPSHOT` are mutable; pin verified commits or tags
for reproducible consumption. A Gradle SNAPSHOT property does not create a tag.

## Verification before synchronization

1. Review the public ABI and Java/Kotlin consumer behavior.
2. Run build/ABI/tests for Java 17/21/25 and the publication quality job.
3. Set projectVersion=2.0.0 in a feature/release branch; test that exact SHA.
4. Run release verification with FRESH_VERIFY=true to recompile/reexecute tests.
5. Integrate to dev/main, check the actual target SHA and its pipeline.

The quality job writes artifacts to an isolated Maven directory and launches
separate Java/Kotlin builds with POM-only dependency resolution. This verifies
packaging/transitive dependencies, not JitPack availability or public access.
There must be no failed jobs or unverified rewritten commits before publication.

## Owner synchronization and publication (PowerShell)

Use explicit GitLab remote refs so a stale local branch cannot be published:

```powershell
git fetch gitlab
git fetch github
git log -1 --oneline gitlab/main
git log --oneline gitlab/main..github/main
git log --oneline gitlab/dev..github/dev
```

The last two commands must show no GitHub-only commits for normal fast-forwards.
If either shows commits or fails, inspect it and stop; do not use an unreviewed
force push. Check both remote URLs before sending private changes. Then sync:

```powershell
git push github refs/remotes/gitlab/main:refs/heads/main refs/remotes/gitlab/dev:refs/heads/dev
git fetch github
git rev-parse gitlab/main github/main
git rev-parse gitlab/dev github/dev
```

Verify each pair is identical. Main and dev may legitimately have different SHAs
after a merge; do not require all six refs to be identical. Check GitHub CI before
publishing. Make GitHub public only when ready, after reviewing committed content.

Create the tag at the exact verified main SHA, not whichever local HEAD is active:

```powershell
git tag -a v2.0.0 gitlab/main -m "chore: 2.0.0リリース"
git push gitlab refs/tags/v2.0.0
git push github refs/tags/v2.0.0
```

If the tag already exists, verify it instead of replacing it. Do not use --all,
--mirror or a blanket --tags. Tags and normal commits should use the intended
local author identity; existing GitLab metadata remains unchanged by pushing.

Request the v2.0.0 build on JitPack. Confirm its build log and both published
modules, then compile a separate consumer against the JitPack coordinates above.
Only after that check is the external release verified. Keep the tag fixed.
Subsequent development starts with a new SNAPSHOT and a new version on release.

## Privacy and access

Before the owner makes GitHub public, JitPack needs authorization for a private
repository. No token, subscription or artifact-sharing permission is configured
by these build scripts. Do not commit access credentials.

Sources:
- https://docs.jitpack.io/building/
- https://docs.jitpack.io/private/
- https://semver.org/
