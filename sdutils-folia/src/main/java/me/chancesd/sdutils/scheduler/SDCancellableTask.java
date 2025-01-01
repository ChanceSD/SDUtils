package me.chancesd.sdutils.scheduler;

import com.google.common.base.Preconditions;

public abstract class SDCancellableTask implements Runnable {

	private SDTask task;

	public void cancel() {
		task.cancel();
	}

	public boolean isCancelled() {
		return task.isCancelled();
	}

	public void setTask(final SDTask task) {
		Preconditions.checkNotNull(task);
		this.task = task;
	}

}
