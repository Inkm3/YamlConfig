plugins {
    application
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
}

val smokeRepository = providers.gradleProperty("smokeRepository").get()
val libraryVersion = providers.gradleProperty("libraryVersion").get()
repositories {
    exclusiveContent {
        forRepository {
            maven {
                url = uri(smokeRepository)
                metadataSources {
                    mavenPom()
                    artifact()
                    ignoreGradleMetadataRedirection()
                }
            }
        }
        filter { includeGroup("com.github.inkm3.yamlconfig") }
    }
    mavenCentral()
}
dependencies {
    implementation("com.github.inkm3.yamlconfig:snakeyaml:$libraryVersion")
}
kotlin { jvmToolchain(17) }
application { mainClass.set("example.KotlinConsumerKt") }
