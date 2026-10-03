package com.github.inkm3.yamlconfig.source

import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ClasspathYamlInputTest {
    @Test fun rejectsBlankAndAbsoluteResourceNames() {
        assertFailsWith<IllegalArgumentException> { ClasspathYamlInput(javaClass.classLoader, "") }
        assertFailsWith<IllegalArgumentException> { ClasspathYamlInput(javaClass.classLoader, "/config.yml") }
    }

    @Test fun missingResourceThrowsFileNotFound() {
        val loader = object : ClassLoader(null) {}
        assertFailsWith<FileNotFoundException> { ClasspathYamlInput(loader, "missing.yml").openReader() }
    }

    @Test fun readsResourceWithProvidedClassLoader() {
        val loader = object : ClassLoader(null) {
            override fun getResourceAsStream(name: String) =
                if (name == "config.yml") ByteArrayInputStream("hello".toByteArray()) else null
        }
        val input = ClasspathYamlInput(loader, "config.yml")
        assertEquals("classpath:config.yml", input.description)
        assertEquals("hello", input.openReader().use { it.readText() })
    }
}
