package com.github.inkm3.yamlconfig.save

import com.github.inkm3.yamlconfig.context.YamlWriteContext
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.YamlSaveContext
import com.github.inkm3.yamlconfig.save.internal.YamlSaveValue
import com.github.inkm3.yamlconfig.save.internal.action.YamlSaveActionResolver
import com.github.inkm3.yamlconfig.save.internal.action.YamlWriteAction
import com.github.inkm3.yamlconfig.schema.int
import com.github.inkm3.yamlconfig.testsupport.TestYamlEditor
import com.github.inkm3.yamlconfig.testsupport.i
import kotlin.test.Test
import kotlin.test.assertEquals

class YamlSaveActionResolverTest {
    private val path = YamlPath.root().child("value")

    @Test fun preserveOverridesKeepsUnchangedAndSetsChanged() {
        val context = context(YamlSaveMode.PRESERVE_OVERRIDES, null)
        assertEquals(YamlWriteAction.KEEP, YamlSaveActionResolver.resolve(int(), context, path, 1, 1, YamlSaveValue.Missing))
        assertEquals(YamlWriteAction.SET, YamlSaveActionResolver.resolve(int(), context, path, 2, 1, YamlSaveValue.Missing))
    }

    @Test fun minimalDifferenceRemovesValueEqualToYamlDefaultBeforeBaselineCheck() {
        val defaults = com.github.inkm3.yamlconfig.testsupport.stringMappingOf("value" to i(5))
        val context = context(YamlSaveMode.MINIMAL_DIFFERENCE, defaults)
        assertEquals(
            YamlWriteAction.REMOVE,
            YamlSaveActionResolver.resolve(int(), context, path, 5, 5, YamlSaveValue.Missing),
        )
    }

    @Test fun minimalDifferenceUsesFallbackDefaultOnlyWhenYamlDefaultIsMissing() {
        val context = context(YamlSaveMode.MINIMAL_DIFFERENCE, null)
        assertEquals(
            YamlWriteAction.REMOVE,
            YamlSaveActionResolver.resolve(int(), context, path, 7, 1, YamlSaveValue.Present(7)),
        )
    }

    @Test fun minimalDifferenceKeepsUnchangedNonDefault() {
        val defaults = com.github.inkm3.yamlconfig.testsupport.stringMappingOf("value" to i(5))
        val context = context(YamlSaveMode.MINIMAL_DIFFERENCE, defaults)
        assertEquals(
            YamlWriteAction.KEEP,
            YamlSaveActionResolver.resolve(int(), context, path, 9, 9, YamlSaveValue.Missing),
        )
    }

    private fun context(mode: YamlSaveMode, defaults: com.github.inkm3.yamlconfig.node.YamlNode?): YamlSaveContext =
        YamlSaveContext(YamlWriteContext(TestYamlEditor(com.github.inkm3.yamlconfig.testsupport.stringMappingOf("value" to i(1)))), mode, defaults)
}
