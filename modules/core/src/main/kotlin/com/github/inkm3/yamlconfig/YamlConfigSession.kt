package com.github.inkm3.yamlconfig

import com.github.inkm3.yamlconfig.context.YamlWriteContext
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.save.internal.YamlSaveContext
import com.github.inkm3.yamlconfig.save.internal.YamlSaveDispatcher
import com.github.inkm3.yamlconfig.save.internal.YamlSaveValue
import com.github.inkm3.yamlconfig.save.internal.YamlSnapshot
import com.github.inkm3.yamlconfig.schema.YamlSchema
import com.github.inkm3.yamlconfig.source.YamlOutput
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.YamlEngine

public class YamlConfigSession<T> internal constructor(
    public var value: T,

    private val schema: YamlSchema<T>,
    private val defaultsRoot: YamlNode?,
    private var userEditor: YamlEditor,
    private val engine: YamlEngine,
    private val output: YamlOutput,
    private val saveMode: YamlSaveMode,
) {

    private var baseline: T = YamlSnapshot.copy(schema, value)

    public fun save(): Unit {
        val workingEditor = userEditor.fork()
        val saveContext = YamlSaveContext(
            YamlWriteContext(workingEditor),
            saveMode,
            defaultsRoot,
        )

        YamlSaveDispatcher.save(
            schema,
            saveContext,
            YamlPath.root(),
            value,
            baseline,
            YamlSaveValue.Missing,
        )

        val newBaseline = YamlSnapshot.copy(schema, value)

        engine.write(workingEditor, output)
        userEditor = workingEditor
        baseline = newBaseline
    }
}