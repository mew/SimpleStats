package zone.nora.simplestats

import com.google.gson.Gson
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import zone.nora.simplestats.commands.SetKeyCommand
import zone.nora.simplestats.commands.StatsCommand
import zone.nora.simplestats.config.Config
import java.util.*

@Mod(
    modid = SimpleStats.MODID,
    name = SimpleStats.MOD_NAME,
    version = SimpleStats.MOD_VERSION,
    modLanguage = "kotlin",
    modLanguageAdapter = "gg.essential.api.utils.KotlinAdapter"
) object SimpleStats {
    const val MODID: String = "simplestats"
    const val MOD_NAME: String = "SimpleStats"
    const val MOD_VERSION: String = "2.0"

    val gson: Gson = Gson()
    val uuidMap: HashMap<String, UUID> = HashMap()

    @EventHandler
    fun onPreInit(event: FMLPreInitializationEvent): Unit = Config.preload()

    @EventHandler
    fun onInit(event: FMLInitializationEvent): Unit = ClientCommandHandler.instance.run {
        registerCommand(StatsCommand)
        registerCommand(SetKeyCommand)
    }
}