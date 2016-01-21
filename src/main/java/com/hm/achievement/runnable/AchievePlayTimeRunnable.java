package com.hm.achievement.runnable;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.hm.achievement.AdvancedAchievements;

public class AchievePlayTimeRunnable implements Runnable {

	private AdvancedAchievements plugin;
	// List of achievements extracted from configuration.
	private int[] achievementPlayTimes;
	// Set corresponding to whether a player has obtained a specific
	// play time achievement.
	// Used as pseudo-caching system to reduce load on database.
	private HashSet<?>[] playerAchievements;

	public AchievePlayTimeRunnable(AdvancedAchievements plugin) {

		this.plugin = plugin;

		extractAchievementsFromConfig(plugin);

	}

	/**
	 * Load list of achievements from configuration.
	 */
	public void extractAchievementsFromConfig(AdvancedAchievements plugin) {

		achievementPlayTimes = new int[plugin.getConfig().getConfigurationSection("PlayedTime").getKeys(false).size()];
		int i = 0;
		for (String playedTime : plugin.getConfig().getConfigurationSection("PlayedTime").getKeys(false)) {
			achievementPlayTimes[i] = Integer.valueOf(playedTime);
			i++;
		}

		playerAchievements = new HashSet<?>[achievementPlayTimes.length];
		for (i = 0; i < playerAchievements.length; ++i)
			playerAchievements[i] = new HashSet<Player>();

	}

	@Override
	public void run() {

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			registerTimes(player);
		}
	}

	/**
	 * Update play times and store them into server's memory until player
	 * disconnects.
	 * 
	 * @param player
	 */
	@SuppressWarnings("unchecked")
	public void registerTimes(Player player) {

		// Do not register any times if player does not have permission.
		if (!player.hasPermission("achievement.count.playedtime"))
			return;

		// Extra check in case server was reloaded and players did not
		// reconnect.
		if (!plugin.getConnectionListener().getJoinTime().containsKey(player)) {
			plugin.getConnectionListener().getJoinTime().put(player, System.currentTimeMillis());
			plugin.getConnectionListener().getPlayTime().put(player, plugin.getDb().updateAndGetPlaytime(player, 0L));
		} else {
			for (int i = 0; i < achievementPlayTimes.length; i++) {
				if (System.currentTimeMillis() - plugin.getConnectionListener().getJoinTime().get(player)
						+ plugin.getConnectionListener().getPlayTime().get(player) > achievementPlayTimes[i] * 3600000
						&& !playerAchievements[i].contains(player)) {
					if (!plugin.getDb().hasPlayerAchievement(player,
							plugin.getConfig().getString("PlayedTime." + achievementPlayTimes[i] + ".Name"))) {

						plugin.getAchievementDisplay().displayAchievement(player,
								"PlayedTime." + achievementPlayTimes[i]);
						plugin.getDb().registerAchievement(player,
								plugin.getConfig().getString("PlayedTime." + achievementPlayTimes[i] + ".Name"),
								plugin.getConfig().getString("PlayedTime." + achievementPlayTimes[i] + ".Message"));
						plugin.getReward().checkConfig(player, "PlayedTime." + achievementPlayTimes[i]);

					}
					((HashSet<Player>) playerAchievements[i]).add(player);
				}
			}
		}
	}

	public HashSet<?>[] getPlayerAchievements() {

		return playerAchievements;
	}

}
