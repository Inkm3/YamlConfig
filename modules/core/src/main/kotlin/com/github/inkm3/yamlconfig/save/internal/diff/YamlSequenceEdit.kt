package com.github.inkm3.yamlconfig.save.internal.diff

internal sealed interface YamlSequenceEdit<T> {

    data class Insert<T>(
        val index: Int,
        val value: T,
    ): YamlSequenceEdit<T>

    data class Update<T>(
        val index: Int,
        val baseline: T,
        val current: T,
    ): YamlSequenceEdit<T>

    data class Move<T>(
        val fromIndex: Int,
        val toIndex: Int,
    ): YamlSequenceEdit<T>

    data class Remove<T>(
        val index: Int,
    ): YamlSequenceEdit<T>
}