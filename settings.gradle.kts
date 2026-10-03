import PathNode.PathKind

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }
}

rootProject.name = "YamlConfig"



includePaths("modules") {
    module("core")
    module("snakeyaml")
//    module("test-support")
}



fun includePaths(rootPath: String, block: PathNode.() -> Unit = {}) {
    val rootNode = PathNode(rootPath, PathKind.DIRECTORY)
    rootNode.apply(block)

    rootNode.children.forEach { includePath(rootPath, it, emptyList()) }
}

fun includePath(rootPath: String, node: PathNode, parents: List<String>) {
    val segments = parents + node.key

    val projectPath = ":${segments.joinToString(":")}"
    val projectDir = (segments.fold(file(rootPath)) { parent, segment ->
        parent.resolve(segment)
    })

    include(projectPath)
    project(projectPath).projectDir = projectDir

    when (node.kind) {
        PathKind.MODULE -> {}
        PathKind.MODULES -> projectDir.listFiles()?.forEach { includePath(rootPath, PathNode(it.name, PathKind.MODULE), segments) }
        PathKind.DIRECTORY -> node.children.forEach { includePath(rootPath, it, segments) }
    }
}

open class PathNode(val key: String, val kind: PathKind) {
    val children = mutableListOf<PathNode>()

    enum class PathKind {
        MODULE,
        MODULES,
        DIRECTORY;
    }

    fun module(name: String) {
        children += PathNode(name, PathKind.MODULE)
    }

    fun modules(name: String) {
        children += PathNode(name, PathKind.MODULES)
    }

    fun directory(name: String, block: PathNode.() -> Unit = {}) {
        children += PathNode(name, PathKind.DIRECTORY).apply(block)
    }
}