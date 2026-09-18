
plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":core"))

    implementation(libs.snakeyaml.engine)

    testImplementation(kotlin("test"))
}