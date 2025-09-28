package me.chancesd.sdutils.tasks;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import me.chancesd.sdutils.scheduler.ScheduleUtils;

public abstract class PausableTask implements Runnable {
	private long finishTime;
	private long duration;
	private ScheduledFuture<?> task;
	private boolean paused;
	private long pausedAt;
	private boolean expired;

	protected PausableTask(final long duration) {
		this.finishTime = System.currentTimeMillis() + duration;
		this.duration = duration;
		scheduleTask(duration);
	}

	protected abstract void onComplete();

	@Override
	public final void run() {
		this.expired = true;
		onComplete();
	}

	protected void scheduleTask(final long timeMs) {
		task = ScheduleUtils.runAsyncLater(this, timeMs, TimeUnit.MILLISECONDS);
	}

	public synchronized void pause() {
		if (!paused) {
			task.cancel(false);
			pausedAt = System.currentTimeMillis();
			paused = true;
		}
	}

	public synchronized void resume() {
		if (paused) {
			final long timeLeft = getTimeleft();
			scheduleTask(timeLeft);
			finishTime = System.currentTimeMillis() + timeLeft;
			paused = false;
		}
	}

	public synchronized void cancel() {
		if (task != null) {
			task.cancel(false);
		}
	}

	public boolean isExpired() {
		return this.expired;
	}

	public long getTimeleft() {
		if (paused) {
			final long pauseDuration = System.currentTimeMillis() - pausedAt;
			return Math.max(0, finishTime + pauseDuration - System.currentTimeMillis());
		}
		return Math.max(0, finishTime - System.currentTimeMillis());
	}

	public long getFinishTime() {
		return finishTime;
	}

	public long getDuration() {
		return duration;
	}

}
