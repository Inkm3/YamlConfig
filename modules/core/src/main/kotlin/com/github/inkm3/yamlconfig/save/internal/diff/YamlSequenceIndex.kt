package com.github.inkm3.yamlconfig.save.internal.diff

/** Mutable position index owned by one calculation; entries use reference identity. */
internal class YamlSequenceIndex<T>(
    initial: List<T>,
    private val hash: (T) -> Int,
    private val identity: (T) -> Any?,
) {
    internal class Entry<T>(val value: T, var position: Int, val hash: Int, val identity: Any?)
    private val entries = ArrayList<Entry<T>>(initial.size)
    private val exact = HashMap<Int, MutableSet<Entry<T>>>()
    private val identities = HashMap<Any, MutableSet<Entry<T>>>()
    internal val size: Int get() = entries.size
    internal operator fun get(index: Int): Entry<T> = entries[index]

    init { initial.forEach { insert(entries.size, it) } }

    internal fun findExact(start: Int, target: T, targetHash: Int, equal: (T, T) -> Boolean): Int =
        first(exact[targetHash], start) { equal(it.value, target) }

    internal fun findIdentity(start: Int, key: Any?): Int =
        if (key == null) -1 else first(identities[key], start) { true }

    private inline fun first(bucket: Set<Entry<T>>?, start: Int, accepts: (Entry<T>) -> Boolean): Int {
        var result = Int.MAX_VALUE
        bucket?.forEach { entry ->
            if (entry.position >= start && entry.position < result && accepts(entry)) result = entry.position
        }
        return if (result == Int.MAX_VALUE) -1 else result
    }

    internal fun insert(index: Int, value: T) {
        val entry = Entry(value, index, hash(value), identity(value))
        entries.add(index, entry)
        exact.getOrPut(entry.hash) { LinkedHashSet() }.add(entry)
        entry.identity?.let { identities.getOrPut(it) { LinkedHashSet() }.add(entry) }
        positions(index + 1, entries.lastIndex)
    }

    internal fun remove(index: Int): Entry<T> {
        val entry = entries.removeAt(index)
        unregister(entry)
        positions(index, entries.lastIndex)
        return entry
    }

    internal fun replace(index: Int, value: T): T {
        val old = entries[index]
        unregister(old)
        val entry = Entry(value, index, hash(value), identity(value))
        entries[index] = entry
        exact.getOrPut(entry.hash) { LinkedHashSet() }.add(entry)
        entry.identity?.let { identities.getOrPut(it) { LinkedHashSet() }.add(entry) }
        return old.value
    }

    internal fun move(from: Int, to: Int) {
        entries.add(to, entries.removeAt(from))
        positions(minOf(from, to), maxOf(from, to))
    }

    private fun unregister(entry: Entry<T>) {
        exact[entry.hash]?.let { if (it.remove(entry) && it.isEmpty()) exact.remove(entry.hash) }
        entry.identity?.let { key ->
            identities[key]?.let { if (it.remove(entry) && it.isEmpty()) identities.remove(key) }
        }
    }

    private fun positions(start: Int, end: Int) {
        for (index in start..end) entries[index].position = index
    }
}
