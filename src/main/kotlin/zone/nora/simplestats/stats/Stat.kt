package zone.nora.simplestats.stats

import zone.nora.simplestats.dsl.lowerSnakeToUpper

data class Stat<T>(
    val key: String,
    val display: String = key.lowerSnakeToUpper(),
    val colour: Char? = null,
    inline val action: (Stat<T>.(stat: T) -> Any)? = null
) {
    companion object {
        @JvmField
        val COINS: Stat<Int> = Stat("coins")
    }
}
