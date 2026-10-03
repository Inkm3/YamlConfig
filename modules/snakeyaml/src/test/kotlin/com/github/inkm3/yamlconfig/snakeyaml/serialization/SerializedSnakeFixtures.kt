package com.github.inkm3.yamlconfig.snakeyaml.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SnakeSavedDatabase(val host: String = "localhost", val port: Int = 3306)

@Serializable
internal data class SnakeSavedConfig(
    @SerialName("server-name") val name: String = "lobby",
    val port: Int = 25565,
    val database: SnakeSavedDatabase = SnakeSavedDatabase(host = "prod"),
    val servers: List<String> = emptyList(),
    val limits: Map<String, Int> = emptyMap(),
    val message: String? = "hello",
)
