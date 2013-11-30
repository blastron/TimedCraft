package timedCraft;

import java.util.logging.Logger;

import net.minecraftforge.common.Configuration;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = TimedCraft.modid, name="TimedCraft", version="0.1")
@NetworkMod(clientSideRequired = false, serverSideRequired = false)
public class TimedCraft {
	public static final String modid = "TimedCraft";
	
	@Instance(modid)
	public static TimedCraft Instance;
	
	public static Logger Logger;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Logger = Logger.getLogger(modid);
		Logger.setParent(FMLLog.getLogger());
		
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		TimedConfiguration.Load(config);
	}
	
	@EventHandler
	public void load(FMLInitializationEvent event) {
		GameRegistry.registerPlayerTracker(new TimedPlayerTracker());
		TickRegistry.registerTickHandler(new TimedTickHandler(), Side.SERVER);
	}
}
