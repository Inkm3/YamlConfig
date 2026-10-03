package com.github.inkm3.yamlconfig.save.serialized.performance

import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.testsupport.TestYamlEditor
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SavePlannerProfileTest {
    private fun assertEquivalent(case: SaveProfileCase) {
        val old = case.run(reference = true)
        val new = case.run(reference = false)
        assertEquals(old.error?.javaClass, new.error?.javaClass, case.name)
        assertEquals(old.error?.message, new.error?.message, case.name)
        assertEquals(old.candidate?.root.toString(), new.candidate?.root.toString(), case.name)
        assertTrue(new.reloads <= old.reloads, "${case.name}: ${new.reloads} > ${old.reloads}")
        val candidate = new.candidate ?: return
        val editor = TestYamlEditor(case.user)
        var pure = case.user
        candidate.patches.forEach { it.apply(editor); pure = it.applyTo(pure) }
        assertEquals(candidate.root.toString(), pure.toString())
        assertEquals(candidate.root.toString(), editor.root.toString())
        assertEquals(case.expected, case.normalize(editor.root))
    }

    @Test fun representativeResultsAndActualEditsMatchTheFrozenControl() {
        SaveProfileFixtures.cases().forEach(::assertEquivalent)
    }

    @Test fun referenceExposesRepeatedValidationButSimpleSuccessNeedsOnlyOne() {
        val cases = SaveProfileFixtures.cases()
        assertEquals(1, cases.first { it.name == "scalar-preserve" }.run(true).reloads)
        val failure = cases.first { it.name == "large-unrepresentable" }.run(true)
        assertNotNull(failure.error)
        assertTrue(failure.reloads > 1)
    }

    @Test fun smallDependentAndOmittedCombinationsKeepTheSameOutcome() {
        val serializer = serializer<ProfileConfig>()
        var count = 0
        for (mode in YamlSaveMode.entries) for (base in listOf(10, 30)) {
            for (port in listOf(25565, 40000)) for (yamlPort in listOf(25565, 30000)) {
                for (explicitDerived in listOf(false, true)) for (newBase in listOf(10, 30)) {
                    val before = ProfileConfig("t", base, base + 1, port)
                    val entries = linkedMapOf("token" to s("t"), "base" to i(base), "port" to i(port))
                    if (explicitDerived) entries["derived"] = i(base + 1)
                    val case = SaveProfileFixtures.case("combination-$count", serializer, before,
                        before.copy(base = newBase, port = 25565), stringMappingOf(*entries.toList().toTypedArray()),
                        stringMappingOf("port" to i(yamlPort)), mode)
                    assertEquivalent(case)
                    count++
                }
            }
        }
        assertEquals(64, count)
    }

    @Test fun diagnosticProfileHasNoWallClockPassFailThreshold() {
        val cases = SaveProfileFixtures.cases()
        cases.forEach(::assertEquivalent)
        if (!java.lang.Boolean.getBoolean("yamlconfig.profileSavePlanner")) return
        println("SAVE_PROFILE_ENV|java=${System.getProperty("java.runtime.version")}|vm=${System.getProperty("java.vm.name")}" +
            "|processors=${Runtime.getRuntime().availableProcessors()}|warmup=30|samples=7|operations=20")
        for (case in cases) {
            repeat(30) { case.run(true); case.run(false) }
            val reference = LongArray(7)
            val current = LongArray(7)
            for (sample in 0 until 7) {
                // Alternate order to reduce (not eliminate) warmup/GC/order bias.
                if (sample % 2 == 0) {
                    reference[sample] = time(case, true); current[sample] = time(case, false)
                } else {
                    current[sample] = time(case, false); reference[sample] = time(case, true)
                }
            }
            println("SAVE_PROFILE|${case.name}|referenceCalls=${case.run(true).reloads}|currentCalls=${case.run(false).reloads}" +
                "|referenceMedianNs=${reference.sorted()[3]}|currentMedianNs=${current.sorted()[3]}" +
                "|referenceSamplesNs=${reference.joinToString(",")}|currentSamplesNs=${current.joinToString(",")}")
        }
    }

    private fun time(case: SaveProfileCase, reference: Boolean): Long {
        val started = System.nanoTime()
        var observed = 0
        repeat(20) {
            val result = case.run(reference)
            observed += result.reloads + (result.candidate?.patches?.size ?: 0)
        }
        sink = observed
        return (System.nanoTime() - started) / 20
    }

    companion object { @Volatile private var sink: Int = 0 }
}
