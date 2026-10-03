package com.github.inkm3.yamlconfig.save.serialized.performance.reduction

import com.sun.management.ThreadMXBean
import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertEquals

/** Opt-in diagnostics use the existing profile flag. No wall-clock assertions. */
class MinimalReductionProfileTest {
    @Test fun compareConstructionTimeAndThreadAllocations() {
        val cases = ReductionFixtures.cases()
        cases.forEach { case ->
            assertEquals(ReductionFixtures.run(case, true, true).observations,
                ReductionFixtures.run(case, false, true).observations)
        }
        if (!java.lang.Boolean.getBoolean("yamlconfig.profileSavePlanner")) return
        val bean = (ManagementFactory.getThreadMXBean() as? ThreadMXBean)?.takeIf {
            it.isThreadAllocatedMemorySupported && it.isThreadAllocatedMemoryEnabled
        }
        println("REDUCTION_PROFILE_ENV|java=${System.getProperty("java.runtime.version")}|warmup=50|samples=7|operations=20" +
            "|threadAllocationAvailable=${bean != null}")
        for (case in cases) {
            repeat(50) { ReductionFixtures.run(case, true); ReductionFixtures.run(case, false) }
            val old = ArrayList<Sample>(); val new = ArrayList<Sample>()
            repeat(7) { sample ->
                if (sample % 2 == 0) { old += measure(case, true, bean); new += measure(case, false, bean) }
                else { new += measure(case, false, bean); old += measure(case, true, bean) }
            }
            println("REDUCTION_PROFILE|${case.name}|referenceChecks=${ReductionFixtures.run(case, true).checks}" +
                "|currentChecks=${ReductionFixtures.run(case, false).checks}" +
                "|referenceMedianNs=${old.map { it.ns }.sorted()[3]}|currentMedianNs=${new.map { it.ns }.sorted()[3]}" +
                "|referenceMedianBytes=${old.map { it.bytes }.sorted()[3]}|currentMedianBytes=${new.map { it.bytes }.sorted()[3]}" +
                "|referenceNs=${old.joinToString(",") { it.ns.toString() }}|currentNs=${new.joinToString(",") { it.ns.toString() }}" +
                "|referenceBytes=${old.joinToString(",") { it.bytes.toString() }}|currentBytes=${new.joinToString(",") { it.bytes.toString() }}")
        }
    }

    private data class Sample(val ns: Long, val bytes: Long)
    private fun measure(case: ReductionCase, reference: Boolean, bean: ThreadMXBean?): Sample {
        val allocated = bean?.currentThreadAllocatedBytes ?: -1
        val started = System.nanoTime()
        repeat(20) { sink = ReductionFixtures.run(case, reference).candidate.root }
        val elapsed = System.nanoTime() - started
        val end = bean?.currentThreadAllocatedBytes ?: -1
        return Sample(elapsed / 20, if (allocated < 0 || end < 0) -1 else (end - allocated) / 20)
    }
    companion object { @Volatile private var sink: Any? = null }
}
