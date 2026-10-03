plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

description = "Non-published JMH benchmarks for YamlConfig"

dependencies {
    implementation(project(":snakeyaml"))
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

val resultFile = layout.buildDirectory.file("reports/jmh/results.json")
val filter = providers.gradleProperty("jmhFilter").orElse(".*ConfigBenchmark.*")
val forks = providers.gradleProperty("jmhForks").orElse("2")
val warmups = providers.gradleProperty("jmhWarmupIterations").orElse("3")
val iterations = providers.gradleProperty("jmhMeasurementIterations").orElse("5")
val launcher = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) }

tasks.register<JavaExec>("jmh") {
    group = "verification"
    description = "Runs forked benchmarks; intentionally excluded from build/check"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    javaLauncher.set(launcher)
    val output = resultFile.get().asFile
    doFirst { output.parentFile.mkdirs() }
    args(filter.get(), "-f", forks.get(), "-wi", warmups.get(), "-i", iterations.get(),
        "-w", "300ms", "-r", "300ms", "-prof", "gc", "-foe", "true",
        "-rf", "json", "-rff", output.absolutePath)
}
