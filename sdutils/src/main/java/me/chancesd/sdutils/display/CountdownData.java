package me.chancesd.sdutils.display;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import me.chancesd.sdutils.display.DisplayManager.MessageSource;
import me.chancesd.sdutils.display.DisplayManager.TimeProgressSource;
import me.chancesd.sdutils.utils.Log;

public class CountdownData {
	private final TimeProgressSource progressSource;
	private final MessageSource bossBarSource;
	private final MessageSource actionBarSource;
	final BossBar bossBar;
	private final ProgressBar progressBar;
	private final Runnable onFinish;

	private CountdownData(final Builder builder) {
		this.bossBar = builder.bossBar;
		this.progressSource = builder.progressSource;
		this.bossBarSource = builder.bossBarSource;
		this.actionBarSource = builder.actionBarSource;
		this.progressBar = builder.progressBar;
		this.onFinish = builder.onFinish;
	}

	boolean update() {
		final long timePassedMs = progressSource.getProgress();
		final long totalTimeMs = progressSource.getGoal();

		if (timePassedMs >= totalTimeMs) {
			if (onFinish != null) {
				onFinish.run();
			}
			return true;
		}

		// Check for negative timePassed which causes progress > 1.0
		if (timePassedMs < 0) {
			Log.severe("TimePassed is negative: " + timePassedMs);
			return true;
		}

		if (actionBarSource != null) {
			progressBar.setProgress(timePassedMs).setGoal(totalTimeMs).calculate();
			actionBarSource.getMessage(progressSource);
		}

		if (bossBarSource != null) {
			bossBar.setTitle(bossBarSource.getMessage(progressSource));
			bossBar.setProgress((totalTimeMs - timePassedMs) / (double) totalTimeMs);
		}

		return false;
	}

	public static class Builder {
		private TimeProgressSource progressSource;
		private MessageSource bossBarSource;
		private MessageSource actionBarSource;
		private BossBar bossBar;
		private ProgressBar progressBar;
		private Runnable onFinish;

		public Builder withBossBar(final BossBar bar, final MessageSource msgSource) {
			this.bossBar = bar;
			this.bossBarSource = msgSource;
			return this;
		}

		public Builder withActionBar(final ProgressBar bar, final MessageSource msgSource) {
			this.progressBar = bar;
			this.actionBarSource = msgSource;
			return this;
		}

		public Builder withTimeSource(final TimeProgressSource timeSource) {
			this.progressSource = timeSource;
			return this;
		}

		public Builder onFinish(final Runnable finish) {
			this.onFinish = finish;
			return this;
		}

		public CountdownData build(final Player player) {
			if (bossBar != null) {
				bossBar.addPlayer(player);
			}
			return new CountdownData(this);
		}
	}
}