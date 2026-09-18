package com.github.inkm3.yamlconfig.spi.editor

import com.github.inkm3.yamlconfig.node.YamlNode

public interface YamlCollectionEditor<P> {

    /**
     * Replaces an existing child at [position].
     *
     * The child must already exist.
     */
    public fun replace(position: P, node: YamlNode): Unit

    /**
     * Inserts a new child at [position].
     */
    public fun insert(position: P, node: YamlNode): Unit

    /**
     * Removes the child at [position].
     *
     * Removing a missing child is a no-op.
     */
    public fun remove(position: P): Unit

}