import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension


plugins {
    base

    alias(libs.plugins.kotlin.jvm) apply false
}

group = providers.gradleProperty("projectGroup").get()
version = providers.gradleProperty("projectVersion").get()

subprojects {
    group = rootProject.group
    version = rootProject.version

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmExtension> {
            jvmToolchain(17)

            explicitApi()

            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                javaParameters.set(true)
            }
        }
    }

    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }

            withSourcesJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release.set(17)
            options.encoding = "UTF-8"
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}