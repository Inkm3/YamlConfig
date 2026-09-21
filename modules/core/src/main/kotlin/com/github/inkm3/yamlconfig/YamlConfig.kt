package com.github.inkm3.yamlconfig

import com.github.inkm3.yamlconfig.context.YamlReadContext
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.schema.YamlSchema
import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.source.internal.YamlMissingFileHandler
import com.github.inkm3.yamlconfig.spi.YamlEngine

public class YamlConfig<T> public constructor(
    private val engine: YamlEngine,
    private val userSource: YamlSource,
    private val schema: YamlSchema<T>,
    private val defaultsSource: YamlInput?,
    private val saveMode: YamlSaveMode,
    private val missingFilePolicy: YamlMissingFilePolicy,
) {

    public fun load(): YamlConfigSession<T> {
        YamlMissingFileHandler.handle(
            userSource,
            defaultsSource,
            missingFilePolicy,
        )

        val defaultsDocument = defaultsSource?.let(engine::parse)
        val userDocument = engine.parse(userSource)
        val readContext = YamlReadContext(defaultsDocument?.root, userDocument.root)
        val value = schema.read(readContext, YamlPath.root())

        return YamlConfigSession(
            value,
            schema,
            defaultsDocument?.root,
            userDocument.editor(),
            engine,
            userSource,
            saveMode,
        )
    }
}