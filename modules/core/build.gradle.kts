plugins {
    `java-library`

    alias(libs.plugins.kotlin.jvm)
}

description = "Core API for YamlConfig"

dependencies {
    testImplementation(kotlin("test"))
}