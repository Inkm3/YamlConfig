plugins { application }

// No Kotlin compiler or serialization compiler plugin is applied here.
val smokeRepository = providers.gradleProperty("smokeRepository").get()
val libraryVersion = providers.gradleProperty("libraryVersion").get()
repositories {
    exclusiveContent {
        forRepository {
            maven {
                url = uri(smokeRepository)
                // Exercise published POM transitive dependencies, not project substitution.
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
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}
application { mainClass.set("example.JavaConsumer") }
