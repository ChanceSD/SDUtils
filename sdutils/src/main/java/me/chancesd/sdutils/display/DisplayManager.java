package me.chancesd.sdutils.display;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;

import me.chancesd.sdutils.scheduler.ScheduleUtils;

public class DisplayManager implements Runnable {

	protected final Map<Player, List<CountdownData>> countdowns = new ConcurrentHashMap<>();
	private ScheduledFuture<?> timer;

	public void createCountdown(final Player player, final CountdownData countdownData) {
		countdowns.computeIfAbsent(player, k -> new ArrayList<>()).add(countdownData);
		if (timer == null) {
			timer = ScheduleUtils.runAsyncTimer(this, 0, 100, TimeUnit.MILLISECONDS);
		}
	}

	public void cancelCountdown(final Player player, final CountdownData countdownData) {
		final List<CountdownData> dataList = countdowns.get(player);
		if (dataList != null && dataList.remove(countdownData)) {
			if (countdownData.bossBar != null) {
				countdownData.bossBar.removePlayer(player);
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
				data.bossBar.removePlayer(player);
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
					data.bossBar.removePlayer(player);
				}
				return shouldRemove;
			});

			if (dataList.isEmpty()) {
				it.remove();
			}
		}

		if (countdowns.isEmpty()) {
			timer.cancel(false);
			timer = null;
		}
	}

	public interface TimeProgressSource {
		double getProgress();
		long getGoal();
	}

	public interface MessageSource {
		String getMessage(TimeProgressSource timeSource);
	}

}
