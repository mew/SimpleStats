package zone.nora.simplestats.config

import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import net.hypixel.api.HypixelAPI
import java.io.File
import java.util.*

object Config : Vigilant(File("./config/simplestats.toml")) {
    @Property(type = PropertyType.TEXT, name = "Hypixel Key", category = "API", protectedText = true, hidden = true)
    var hypixelKeyS: String = ""
        set(value) {
            field = value
            validKey = value.isValidHypixelKey()
        }

    val hypixelKey: UUID get() = if (validKey) UUID.fromString(hypixelKeyS) else UUID.randomUUID()
    var validKey: Boolean = false

    init {
        initialize()

        validKey = hypixelKeyS.isValidHypixelKey()
        registerListener("hypixelKeyS") { newKey: String ->
            validKey = newKey.isValidHypixelKey()
        }
    }

    private fun String.isValidHypixelKey(): Boolean = try {
        val key = UUID.fromString(this)
        HypixelAPI(key).key.get().isSuccess
    } catch (_: Exception) {
        false
    }
}