package com.github.inkm3.yamlconfig.spi.editor

public interface YamlSequenceEditor: YamlCollectionEditor<Int> {

    public val size: Int

    /**
     * Moves an existing element to [toIndex].
     *
     * [toIndex] is the final index after the move.
     */
    public fun move(fromIndex: Int, toIndex: Int): Unit
}