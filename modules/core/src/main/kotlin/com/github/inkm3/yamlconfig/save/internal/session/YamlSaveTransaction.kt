package com.github.inkm3.yamlconfig.save.internal.session

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.source.YamlOutput
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.YamlEngine

/**
 * Advances editor and baseline together, only after write returns successfully.
 * A null preparation is a true no-op: no fork and no write.
 * Baselines must be detached snapshots. Sessions require external synchronization.
 * Output rollback remains the YamlEngine/YamlWriteTransaction implementation's duty.
 */
internal class YamlSaveTransaction<B>(
    baseline: B,
    editor: YamlEditor,
    private val engine: YamlEngine,
    private val output: YamlOutput,
) {
    private class State<B>(val baseline: B, val editor: YamlEditor)
    private var state = State(baseline, editor)
    private var saving = false

    internal fun save(prepare: (B, YamlNode?) -> YamlPreparedSave<B>?) {
        check(!saving) { "Reentrant save is not supported" }
        saving = true
        try {
            val previous = state
            val plan = prepare(previous.baseline, previous.editor.root) ?: return
            val working = previous.editor.fork()
            plan.apply(working)
            val next = State(plan.baseline, working)
            engine.write(working, output)
            state = next
        } finally {
            saving = false
        }
    }
}
