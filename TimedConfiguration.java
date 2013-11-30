package timedCraft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.ConfigCategory;
import net.minecraftforge.common.Configuration;

public class TimedConfiguration {
	public static TimedConfiguration Instance = null;
	
	public static void Load(Configuration config) {
		try {
			config.load();
			
			ConfigCategory category = config.getCategory(Configuration.CATEGORY_GENERAL);
			Instance = new TimedConfiguration(category);
			
			TimedCraft.Logger.info("Successfully loaded configuration.");
		} catch (Exception e) {
			// We didn't successfully read the configuration file
			TimedCraft.Logger.info("Failed to load configuration, defaulting.");
			Instance = new TimedConfiguration();
		} finally {
			config.save();
		}
	}
	
	private Map<String, Integer> timeLimits;
	
	private TimedConfiguration()
	{
		timeLimits = new HashMap<String, Integer>();
	}
	
	private TimedConfiguration(ConfigCategory category)
	{
		timeLimits = new HashMap<String, Integer>();
		
		Iterator<String> keyIterator = category.keySet().iterator();
		while (keyIterator.hasNext()) {
			String currentKey = keyIterator.next();
			timeLimits.put(currentKey, category.get(currentKey).getInt());
		}
	}
	
	// Returns true if the player has a time limit configured, false otherwise.
	public boolean PlayerHasTimeLimit(EntityPlayer player) {
		return this.timeLimits.containsKey(player.username);
	}
	
	// Gets the time, in milliseconds, allowed to the player per day. Returns -1 if the player
	//   is not configured to have a time limit.
	public int GetPlayerDailyTimeAllotment(EntityPlayer player) {
		if (this.timeLimits.containsKey(player.username)) {
			return this.timeLimits.get(player.username);
		} else {
			return -1;
		}
	}
}
