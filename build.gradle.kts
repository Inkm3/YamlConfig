import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

group = providers.gradleProperty("projectGroup").get()
version = providers.gradleProperty("projectVersion").get()
val publishedProjects = setOf("core", "snakeyaml")
val testJavaVersion = providers.gradleProperty("testJavaVersion").map { value ->
    value.toIntOrNull()?.also { require(it in setOf(17, 21, 25)) {
        "testJavaVersion must be one of 17, 21, 25"
    } } ?: error("testJavaVersion must be an integer")
}.orElse(17)

subprojects {
    group = rootProject.group
    version = rootProject.version
    if (name in publishedProjects) apply(plugin = "maven-publish")

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmExtension> {
            jvmToolchain(17)
            if (project.name in publishedProjects) explicitApi()
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                javaParameters.set(true)
            }
        }
    }
    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
            if (project.name in publishedProjects) withSourcesJar()
        }
        if (name in publishedProjects) {
            extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("maven") { from(components["java"]) }
                }
            }
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(17)
            options.encoding = "UTF-8"
        }
        val toolchains = extensions.getByType<JavaToolchainService>()
        val testLauncher = toolchains.launcherFor {
            languageVersion.set(testJavaVersion.map(JavaLanguageVersion::of))
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            javaLauncher.set(testLauncher)
            systemProperty("yamlconfig.expectedTestJavaVersion", testJavaVersion.get())
        }
    }
}
