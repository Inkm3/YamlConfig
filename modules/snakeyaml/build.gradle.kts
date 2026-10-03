
plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core"))

    implementation(libs.snakeyaml.engine)

    testImplementation(kotlin("test"))
}
