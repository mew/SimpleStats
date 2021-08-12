package zone.nora.simplestats.stats

import zone.nora.simplestats.dsl.firstCharUpper

data class Stat<T>(
    val key: String,
    val display: String = key.firstCharUpper().replace('_', ' '),
    val colour: Char? = null,
    inline val action: (Stat<T>.(stat: T) -> Any)? = null
)
