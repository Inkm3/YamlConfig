package com.github.inkm3.yamlconfig.source

import java.io.FileNotFoundException
import java.io.Reader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

public class ClasspathYamlInput public constructor(
    private val classLoader: ClassLoader,
    private val resourceName: String,
    private val charset: Charset = StandardCharsets.UTF_8
):  YamlInput {

    init {
        require(resourceName.isNotBlank()) {
            "Resource path must not be blank"
        }
        require(!resourceName.startsWith("/")) {
            "Resource path must not start with '/': $resourceName"
        }
    }

    override val description: String get() = "classpath:$resourceName"

    override fun openReader(): Reader {
        val stream = classLoader.getResourceAsStream(resourceName)
            ?: throw FileNotFoundException(
                "Classpath resource not found: $resourceName",
            )

        return stream.bufferedReader(charset)
    }
}

public inline fun <reified T : Any> classpathYamlInput(
    resource: String,
    charset: Charset = StandardCharsets.UTF_8,
): ClasspathYamlInput {
    return ClasspathYamlInput(T::class.java.classLoader, resource, charset)
}