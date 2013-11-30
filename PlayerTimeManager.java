package timedCraft;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PlayerTimeManager {
	private static final String dataAllocatedLabel = "TimedCraft_DataAllocated";
	private static final String remainingTimeLabel = "TimedCraft_RemainingTime";
	private static final String lastLogoutTimeLabel = "TimedCraft_LastLogoutTime";
	private static final String lastTickTimeLabel = "TimedCraft_LastTickTime";
	
	private static final int playerNotificationInterval = 300000;
	private static final int playerTimeNearlyUpNotificationTime = 60000;
	
	private static PlayerTimeManager instance = null;
	
	// A redundant data store that we read from whenever the player's stored
	//   entity data is missing, such as on respawn after death.
	private Map<String, Long> cachedPlayerLastTickTime = new HashMap<String, Long>();
	private Map<String, Integer> cachedPlayerTimeRemaining = new HashMap<String, Integer>();
	
	public static PlayerTimeManager GetInstance() {
		if (instance == null) {
			instance = new PlayerTimeManager();
		}
		
		return instance;
	}
	
	public void OnPlayerConnect(EntityPlayer player) {
		if (TimedConfiguration.Instance.PlayerHasTimeLimit(player)) {
			this.EnsurePlayerDataAllocated(player);
			
			// Credit the player for time accumulated since last login
			this.AddTimeAccumulatedSinceLastLogout(player);
			
			// Message the player with their time remaining
			this.SendPlayerTimeNotification(player);
		}
	}
	
	public void OnPlayerDisconnect(EntityPlayer player) {
		if (TimedConfiguration.Instance.PlayerHasTimeLimit(player)) {
			this.EnsurePlayerDataAllocated(player);
			
			// Record the player's logout time
			this.RecordLastLogout(player);
			
			// Flush the last tick time
			player.getEntityData().setLong(lastTickTimeLabel, 0);
		}
	}
	
	public void OnPlayerTick(EntityPlayer player) {
		// If we have a time limit, enforce it
		if (TimedConfiguration.Instance.PlayerHasTimeLimit(player)) {
			this.EnsurePlayerDataAllocated(player);
			
			// Measure the time between ticks and update the stored last tick value
			long lastTickTime = player.getEntityData().getLong(lastTickTimeLabel);
			long currentTickTime = System.currentTimeMillis();
			
			// Only tick the player if we have a last tick
			if (lastTickTime > 0) {
				int millisecondsSinceLastTick = (int) (currentTickTime - player.getEntityData().getLong(lastTickTimeLabel));
				
				int previousTimeRemaining = this.GetPlayerRemainingTime(player);
				
				this.AddTime(player, (int)(millisecondsSinceLastTick * this.GetPlayerTimeRegenerationRate(player)));
				this.SubtractTime(player, millisecondsSinceLastTick);
				
				int currentTimeRemaining = this.GetPlayerRemainingTime(player);
				
				if (currentTimeRemaining <= 0) {
					this.KickPlayer(player);
				} else if (currentTimeRemaining % playerNotificationInterval > previousTimeRemaining % playerNotificationInterval) {
					this.SendPlayerTimeNotification(player);
				} else if (currentTimeRemaining < playerTimeNearlyUpNotificationTime && previousTimeRemaining > playerTimeNearlyUpNotificationTime) {
					this.SendPlayerTimeNotification(player);
				}
			}

			player.getEntityData().setLong(lastTickTimeLabel, currentTickTime);
			this.cachedPlayerLastTickTime.put(player.username, currentTickTime);
		}
	}
	
	// If the player's data is not yet allocated (which occurs either when a new player
    //   joins or the player dies), assign it to either a cached value if we have one or
	//   the defaults. Defaults are:
	//   - remaining time is the player's maximum daily time
	//   - last tick time is null
	//   - last logout is always null (this is zero while the player is logged in, so it doesn't matter if he dies)
	private void EnsurePlayerDataAllocated(EntityPlayer player) {
		boolean dataAllocated = player.getEntityData().getBoolean(dataAllocatedLabel);
		if (!dataAllocated) {
			TimedCraft.Logger.info(String.format("Allocating data for player %s.", player.username));
			
			player.getEntityData().setBoolean(dataAllocatedLabel, true);

			if (this.cachedPlayerTimeRemaining.containsKey(player.username)) {
				player.getEntityData().setInteger(remainingTimeLabel, this.cachedPlayerTimeRemaining.get(player.username));
			} else {
				player.getEntityData().setInteger(remainingTimeLabel, TimedConfiguration.Instance.GetPlayerDailyTimeAllotment(player));
			}
			
			if (this.cachedPlayerLastTickTime.containsKey(player.username)) {
				player.getEntityData().setLong(lastTickTimeLabel, this.cachedPlayerLastTickTime.get(player.username));
			} else {
				player.getEntityData().setLong(lastTickTimeLabel, 0);
			}

			player.getEntityData().setLong(lastLogoutTimeLabel, 0);
		}
	}
	
	// 
	private void AddTimeAccumulatedSinceLastLogout(EntityPlayer player) {
		long lastLogout = player.getEntityData().getLong(lastLogoutTimeLabel);
		if (lastLogout > 0)
		{
			int millisecondsSinceLogout = (int)(System.currentTimeMillis() - lastLogout);
			int millisecondsAccumulated = (int)(millisecondsSinceLogout * this.GetPlayerTimeRegenerationRate(player));
			
			this.AddTime(player, millisecondsAccumulated);
			
			// Clear the last logout
			player.getEntityData().setLong(lastLogoutTimeLabel, 0);
		}
	}
	
	private void RecordLastLogout(EntityPlayer player) {
		player.getEntityData().setLong(lastLogoutTimeLabel, System.currentTimeMillis());
	}
	
	// Adds the given amount of time to the player, up to the per-day cap.
	private void AddTime(EntityPlayer player, int milliseconds) {
		int currentTimeRemaining = this.GetPlayerRemainingTime(player);
		currentTimeRemaining = Math.min(currentTimeRemaining + milliseconds, TimedConfiguration.Instance.GetPlayerDailyTimeAllotment(player));
		this.SetPlayerRemainingTime(player, currentTimeRemaining);
	}
	
	// Subtracts the given amount of time from the player, down to zero.
	private void SubtractTime(EntityPlayer player, int milliseconds) {
		int currentTimeRemaining = this.GetPlayerRemainingTime(player);
		currentTimeRemaining = Math.max(currentTimeRemaining - milliseconds, 0);
		this.SetPlayerRemainingTime(player, currentTimeRemaining);
	}
	
	// Gets the rate at which a player regains play time, compared to the normal passage of time.
	private double GetPlayerTimeRegenerationRate(EntityPlayer player) {
		return TimedConfiguration.Instance.GetPlayerDailyTimeAllotment(player) / 86400000.0;
	}
	
	// Gets the time, in milliseconds, that the player has left to be logged in.
	private int GetPlayerRemainingTime(EntityPlayer player) {
		return player.getEntityData().getInteger(remainingTimeLabel);
	}
	
	// Sets the player's remaining logged-in time, in milliseconds.
	private void SetPlayerRemainingTime(EntityPlayer player, int milliseconds) {
		player.getEntityData().setInteger(remainingTimeLabel, milliseconds);
		this.cachedPlayerTimeRemaining.put(player.username, milliseconds);
	}
	
	private void SendPlayerTimeNotification(EntityPlayer player) {
		int remainingTime = (int) this.GetPlayerRemainingTime(player);
		player.addChatMessage(String.format("You have %d:%02d minutes of gameplay time remaining.", remainingTime / 60000, (remainingTime / 1000) % 60 ));
	}
	
	private void KickPlayer(EntityPlayer player) {
		((EntityPlayerMP)player).playerNetServerHandler.kickPlayerFromServer("Your time is up!");
	}
}
