package com.github.inkm3.yamlconfig.save.internal

internal sealed interface YamlSaveValue<out T> {

    data class Present<T>(
        internal val value: T,
    ): YamlSaveValue<T>

    data object Missing: YamlSaveValue<Nothing>

}

internal inline fun <T, R> YamlSaveValue<T>.map(
    transform: (T) -> R,
): YamlSaveValue<R> {
    return when (this) {
        is YamlSaveValue.Present -> YamlSaveValue.Present(transform(value))
        is YamlSaveValue.Missing -> YamlSaveValue.Missing
    }
}