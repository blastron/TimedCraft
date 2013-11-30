package timedCraft;

import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.IPlayerTracker;

public class TimedPlayerTracker implements IPlayerTracker {

	@Override
	public void onPlayerLogin(EntityPlayer player) {
		PlayerTimeManager.GetInstance().OnPlayerConnect(player);
	}

	@Override
	public void onPlayerLogout(EntityPlayer player) {
		PlayerTimeManager.GetInstance().OnPlayerDisconnect(player);
	}

	@Override
	public void onPlayerChangedDimension(EntityPlayer player) {
		// Do nothing.
	}

	@Override
	public void onPlayerRespawn(EntityPlayer player) {
		// Do nothing.
	}
}
