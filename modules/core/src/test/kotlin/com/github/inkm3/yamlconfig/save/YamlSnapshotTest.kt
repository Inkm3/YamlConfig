package com.github.inkm3.yamlconfig.save

import com.github.inkm3.yamlconfig.save.internal.YamlSnapshot
import com.github.inkm3.yamlconfig.schema.int
import com.github.inkm3.yamlconfig.schema.obj
import com.github.inkm3.yamlconfig.schema.yamlObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class YamlSnapshotTest {
    class Child { var value: Int = 0 }
    class Config { var child: Child = Child() }

    private val childSchema = obj(::Child) { field(Child::value, int()) }
    private val schema = yamlObject(::Config) { field(Config::child, childSchema) }

    @Test fun copyProducesIndependentMutableGraph() {
        val original = Config().apply { child.value = 10 }
        val copy = YamlSnapshot.copy(schema, original)
        assertNotSame(original, copy)
        assertNotSame(original.child, copy.child)
        original.child.value = 20
        assertEquals(10, copy.child.value)
    }
}
