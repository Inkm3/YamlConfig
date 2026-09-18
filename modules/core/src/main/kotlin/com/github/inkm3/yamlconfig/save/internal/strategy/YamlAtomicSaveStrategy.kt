package com.github.inkm3.yamlconfig.save.internal.strategy
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.YamlSaveContext
import com.github.inkm3.yamlconfig.save.internal.YamlSaveValue
import com.github.inkm3.yamlconfig.save.internal.action.YamlSaveActionResolver
import com.github.inkm3.yamlconfig.save.internal.action.YamlWriteAction
import com.github.inkm3.yamlconfig.schema.YamlSchema

internal class YamlAtomicSaveStrategy<T>(
    private val schema: YamlSchema<T>,
): YamlSaveStrategy<T> {

    override fun save(
        context: YamlSaveContext,
        path: YamlPath,
        current: T,
        baseline: T,
        fallbackDefault: YamlSaveValue<T>,
    ): Unit {
        when (YamlSaveActionResolver.resolve(
            schema,
            context,
            path,
            current,
            baseline,
            fallbackDefault,
        )) {
            YamlWriteAction.KEEP -> Unit
            YamlWriteAction.SET -> context.writer.set(path, schema.encode(current))
            YamlWriteAction.REMOVE -> context.writer.remove(path)
        }
    }
}