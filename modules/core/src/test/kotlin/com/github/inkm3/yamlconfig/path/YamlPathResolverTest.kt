package com.github.inkm3.yamlconfig.path

import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class YamlPathResolverTest {
    private val root = stringMappingOf(
        "server" to stringMappingOf(
            "players" to sequenceOf(s("A"), s("B")),
        ),
    )

    @Test fun resolvesNestedNode() {
        val result = YamlPathResolver.lookup(root, YamlPath.root().child("server").child("players").child(1))
        assertEquals(YamlNodeLookupResult.Found(s("B")), result)
    }

    @Test fun missingKeyReportsMissingAtExactPath() {
        val path = YamlPath.root().child("server").child("missing")
        assertEquals(YamlNodeLookupResult.Missing(path), YamlPathResolver.lookup(root, path))
    }

    @Test fun wrongParentKindReportsExpectedMapping() {
        val path = YamlPath.root().child("server").child("players").child(0).child("name")
        val result = YamlPathResolver.lookup(root, path)
        assertIs<YamlNodeLookupResult.ExpectedMapping>(result)
        assertEquals(YamlPath.root().child("server").child("players").child(0), result.path)
    }

    @Test fun outOfBoundsReportsSequenceParentPath() {
        val parent = YamlPath.root().child("server").child("players")
        val result = YamlPathResolver.lookup(root, parent.child(5))
        assertEquals(YamlNodeLookupResult.IndexOutOfBounds(parent, 5, 2), result)
    }

    @Test fun nullRootIsMissing() {
        val path = YamlPath.root().child("x")
        assertEquals(YamlNodeLookupResult.Missing(path), YamlPathResolver.lookup(null, path))
    }
}
