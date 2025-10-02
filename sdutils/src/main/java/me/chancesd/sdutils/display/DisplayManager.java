package me.chancesd.sdutils.display;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import me.chancesd.sdutils.scheduler.ScheduleUtils;

public class DisplayManager implements Runnable {

	private static final String COUNTDOWN_PREFIX = "countdown_";

	protected final Map<Player, List<CountdownData>> countdowns = new ConcurrentHashMap<>();
	protected final Map<Player, Map<String, BossBar>> allBossBars = new ConcurrentHashMap<>();
	private ScheduledFuture<?> timer;

	public void createCountdown(final Player player, final CountdownData countdownData) {
		countdowns.computeIfAbsent(player, k -> new CopyOnWriteArrayList<>()).add(countdownData);

		if (countdownData.bossBar != null) {
			showBossBar(player, COUNTDOWN_PREFIX + countdownData.hashCode(), countdownData.bossBar);
		}

		if (timer == null) {
			timer = ScheduleUtils.runAsyncTimer(this, 0, 100, TimeUnit.MILLISECONDS);
		}
	}

	public void showBossBar(final Player player, final String key, final BossBar bossBar) {
		final Map<String, BossBar> playerBars = allBossBars.computeIfAbsent(player, k -> new ConcurrentHashMap<>());

		// Remove existing bar with same key if it exists
		final BossBar existing = playerBars.remove(key);
		removePlayerFromBossBar(existing, player);

		bossBar.addPlayer(player);
		playerBars.put(key, bossBar);
	}

	public void hideBossBar(final Player player, final String key) {
		final Map<String, BossBar> playerBars = allBossBars.get(player);
		if (playerBars != null) {
			final BossBar bossBar = playerBars.remove(key);
			removePlayerFromBossBar(bossBar, player);
			if (playerBars.isEmpty()) {
				allBossBars.remove(player);
			}
		}
	}

	public void hideAllBossBars(final Player player) {
		final Map<String, BossBar> playerBars = allBossBars.remove(player);
		if (playerBars != null) {
			playerBars.values().forEach(bar -> bar.removePlayer(player));
		}
	}

	public void cancelCountdown(final Player player, final CountdownData countdownData) {
		final List<CountdownData> dataList = countdowns.get(player);
		if (dataList != null && dataList.remove(countdownData)) {
			if (countdownData.bossBar != null) {
				hideBossBar(player, COUNTDOWN_PREFIX + countdownData.hashCode());
			}
			if (dataList.isEmpty()) {
				countdowns.remove(player);
			}
		}
	}

	public void cancelAllCountdown(final Player player) {
		final List<CountdownData> dataList = countdowns.remove(player);
		if (dataList == null) {
			throw new IllegalStateException("Tried to discard countdowns that don't exist!");
		}
		for (final CountdownData data : dataList) {
			if (data.bossBar != null) {
				hideBossBar(player, COUNTDOWN_PREFIX + data.hashCode());
			}
		}
	}

	@Override
	public void run() {
		final Iterator<Map.Entry<Player, List<CountdownData>>> it = countdowns.entrySet().iterator();
		while (it.hasNext()) {
			final Map.Entry<Player, List<CountdownData>> entry = it.next();
			final List<CountdownData> dataList = entry.getValue();
			final Player player = entry.getKey();

			dataList.removeIf(data -> {
				final boolean shouldRemove = data.update();
				if (shouldRemove && data.bossBar != null) {
					hideBossBar(player, COUNTDOWN_PREFIX + data.hashCode());
				}
				return shouldRemove;
			});

			if (dataList.isEmpty()) {
				it.remove();
			}
		}

		if (countdowns.isEmpty() && timer != null) {
			timer.cancel(false);
			timer = null;
		}
	}

	private void removePlayerFromBossBar(final BossBar bossBar, final Player player) {
		if (bossBar != null) {
			bossBar.removePlayer(player);
		}
	}

	public interface TimeProgressSource {
		double getProgress();
		long getGoal();
	}

	public interface MessageSource {
		String getMessage(TimeProgressSource timeSource);
	}

	/**
	 * Cleanup method to be called when shutting down or disabling.
	 * Removes all boss bars from players and clears countdowns.
	 */
	public void cleanup() {
		allBossBars.forEach((player, bars) -> bars.values().forEach(bar -> bar.removePlayer(player)));
		allBossBars.clear();
		countdowns.clear();
	}

}
