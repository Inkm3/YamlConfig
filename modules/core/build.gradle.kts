plugins {
    `java-library`

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

description = "Core API for YamlConfig"

dependencies {
    api(libs.kotlinx.serialization.core)

    testImplementation(kotlin("test"))
}

// Opt-in diagnostics only; ordinary build/test keeps its existing cache behavior.
val profileSavePlanner = providers.gradleProperty("profileSavePlanner")
    .map(String::toBoolean).getOrElse(false)
tasks.test {
    systemProperty("yamlconfig.profileSavePlanner", profileSavePlanner)
    testLogging.showStandardStreams = profileSavePlanner
    if (profileSavePlanner) {
        outputs.upToDateWhen { false }
        outputs.doNotCacheIf("Timing diagnostics must execute, not reuse old measurements") { true }
    }
}
