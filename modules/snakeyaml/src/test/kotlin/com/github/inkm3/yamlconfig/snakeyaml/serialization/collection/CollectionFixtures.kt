package com.github.inkm3.yamlconfig.snakeyaml.serialization.collection

import kotlinx.serialization.Serializable

@Serializable
internal data class CollectionServer(val name: String, val port: Int = 25565)

@Serializable
internal data class CollectionGroup(val name: String, val ports: List<Int>)

@Serializable
internal data class CollectionDependent(val base: Int = 10, val derived: Int = base + 1)

@Serializable
internal data class CollectionDatabase(val host: String = "localhost", val port: Int = 3306)

@Serializable
internal data class CollectionParent(
    val name: String,
    val database: CollectionDatabase = CollectionDatabase(host = "prod"),
)
