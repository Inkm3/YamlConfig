package com.github.inkm3.yamlconfig.save.internal.session

import com.github.inkm3.yamlconfig.spi.YamlEditor

/** A detached next baseline and edits/validation to run before touching output. */
internal class YamlPreparedSave<B>(
    internal val baseline: B,
    internal val apply: (YamlEditor) -> Unit,
)
