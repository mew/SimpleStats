package zone.nora.simplestats.stats

data class Stat<T>(
    val key: String,
    val display: String = key.replaceFirstChar { it.uppercaseChar() }.replace('_', ' '),
    val colour: Char? = null,
    inline val action: (Stat<T>.(stat: T) -> Any)? = null
)
