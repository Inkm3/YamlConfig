package com.github.inkm3.yamlconfig.save.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlScalarRepresentation
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEdit
import com.github.inkm3.yamlconfig.save.serialized.structure.StructuralPlannerFixtures.apply
import com.github.inkm3.yamlconfig.save.serialized.structure.StructuralPlannerFixtures.plan
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class YamlMapValuePlannerTest {
    @Serializable private enum class Key { @SerialName("alpha") A, B }

    @Test fun integerKeyLexemeSurvivesValueChange() {
        val raw = YamlMapKey("0xff", YamlScalarKind.INTEGER)
        val user = YamlMappingNode(mapOf(raw to s("old")))
        val edit = assertIs<YamlStructuralEdit.Mapping>(plan(mapOf(255 to "old"), mapOf(255 to "new"), user))
        assertEquals(setOf(raw), edit.changes.keys)
        apply(user, edit, YamlMappingNode(mapOf(raw to s("new"))))
    }

    @Test fun insertionAndRemovalDoNotReplaceSurvivingValues() {
        val value = stringMappingOf("name" to s("a"), "extra" to s("keep"))
        val user = stringMappingOf("a" to value, "b" to stringMappingOf("name" to s("b")))
        val edit = plan(mapOf("a" to StructuralItem("a"), "b" to StructuralItem("b")),
            mapOf("a" to StructuralItem("a"), "c" to StructuralItem("c")), user)
        apply(user, edit, stringMappingOf("a" to value, "c" to stringMappingOf("name" to s("c"), "port" to i(25565))))
    }

    @Test fun mapObjectValueIsUpdatedRecursively() {
        val value = stringMappingOf("name" to s("a"), "extra" to s("keep"))
        val user = stringMappingOf("a" to value)
        val edit = plan(mapOf("a" to StructuralItem("a")), mapOf("a" to StructuralItem("a", 30000)), user)
        apply(user, edit, stringMappingOf("a" to stringMappingOf("name" to s("a"), "port" to i(30000), "extra" to s("keep"))))
    }

    @Test fun mapOfListsUsesSequenceOperationsInsideExistingEntry() {
        val user = stringMappingOf("a" to sequenceOf(s("x"), s("y")))
        val edit = assertIs<YamlStructuralEdit.Mapping>(plan(mapOf("a" to listOf("x", "y")),
            mapOf("a" to listOf("y", "x")), user))
        assertIs<YamlStructuralEdit.Sequence>(edit.changes.getValue(YamlMapKey("a")))
        apply(user, edit, stringMappingOf("a" to sequenceOf(s("y"), s("x"))))
    }

    @Test fun emptyMapRetainsAnExplicitMapping() {
        val user = stringMappingOf("a" to i(1))
        val edit = assertIs<YamlStructuralEdit.Mapping>(plan(mapOf("a" to 1), emptyMap<String, Int>(), user))
        apply(user, edit, stringMappingOf())
    }

    @Test fun booleanNullAndFloatKeysUseCodecCompatibleSpellings() {
        val booleanKey = YamlMapKey("TRUE", YamlScalarKind.BOOLEAN)
        val nullKey = YamlMapKey("~", YamlScalarKind.NULL)
        val floatKey = YamlMapKey("1", YamlScalarKind.INTEGER)
        val boolUser = YamlMappingNode(mapOf(booleanKey to i(1), nullKey to i(2)))
        apply(boolUser, plan(mapOf<Boolean?, Int>(true to 1, null to 2), mapOf<Boolean?, Int>(true to 3, null to 4), boolUser),
            YamlMappingNode(mapOf(booleanKey to i(3), nullKey to i(4))))
        val floatUser = YamlMappingNode(mapOf(floatKey to i(1)))
        apply(floatUser, plan(mapOf(1.0 to 1), mapOf(1.0 to 2), floatUser), YamlMappingNode(mapOf(floatKey to i(2))))
    }

    @Test fun enumSerializedNameIsTheKeyNotItsKotlinName() {
        val user = stringMappingOf("alpha" to i(1))
        apply(user, plan(mapOf(Key.A to 1), mapOf(Key.A to 2), user), stringMappingOf("alpha" to i(2)))
    }

    @Test fun normalizationCollisionIsNotResolvedByChoosingTheFirstEntry() {
        val keys = setOf(YamlMapKey("1", YamlScalarKind.INTEGER), YamlMapKey("+1", YamlScalarKind.INTEGER))
        assertNull(YamlScalarRepresentation.index(serializer<Int>().descriptor, keys))
    }

    @Test fun transformedCustomKeyMismatchFallsBackToWholeValue() {
        val user = stringMappingOf("lower" to i(1))
        assertIs<YamlStructuralEdit.Set>(plan(mapOf("LOWER" to 1), mapOf("LOWER" to 2), user))
    }

    @Test fun keyOrderAloneDoesNotGenerateAMapRewrite() {
        val user = stringMappingOf("a" to i(1), "b" to i(2))
        assertNull(plan(linkedMapOf("a" to 1, "b" to 2), linkedMapOf("b" to 2, "a" to 1), user))
    }
}
