package me.chancesd.sdutils.display;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import me.chancesd.sdutils.display.DisplayManager.MessageSource;
import me.chancesd.sdutils.display.DisplayManager.TimeProgressSource;

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
		final double timePassed = progressSource.getProgress();
		final long totalTime = progressSource.getGoal();

		if (timePassed >= totalTime) {
			if (onFinish != null) {
				onFinish.run();
			}
			return true;
		}

		if (actionBarSource != null) {
			progressBar.setProgress(timePassed).setGoal(totalTime).calculate();
			actionBarSource.getMessage(progressSource);
		}

		if (bossBarSource != null) {
			bossBar.setTitle(bossBarSource.getMessage(progressSource));
			bossBar.setProgress((totalTime - timePassed) / totalTime);
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