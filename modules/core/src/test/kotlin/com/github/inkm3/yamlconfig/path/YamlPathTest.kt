package com.github.inkm3.yamlconfig.path

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class YamlPathTest {
    @Test fun rootFormatting() = assertEquals("$", YamlPath.root().toString())

    @Test fun simpleAndIndexedPathFormatting() {
        val path = YamlPath.root().child("server").child("players").child(2).child("name")
        assertEquals("$.server.players[2].name", path.toString())
    }

    @Test fun specialStringKeyIsQuotedAndEscaped() {
        val path = YamlPath.root().child("a b\"c")
        assertEquals("${'$'}{\"a b\\\"c\"}", path.toString())
    }

    @Test fun typedKeysAreDistinctAndFormatted() {
        val intKey = YamlMapKey("1", YamlScalarKind.INTEGER)
        val stringKey = YamlMapKey("1", YamlScalarKind.STRING)
        assertNotEquals(intKey, stringKey)
        assertEquals("${'$'}{1}", YamlPath.root().child(intKey).toString())
        assertEquals("${'$'}{\"1\"}", YamlPath.root().child(stringKey).toString())
    }

    @Test fun negativeIndexIsRejected() {
        assertFailsWith<IllegalArgumentException> { YamlPath.root().child(-1) }
    }
}
