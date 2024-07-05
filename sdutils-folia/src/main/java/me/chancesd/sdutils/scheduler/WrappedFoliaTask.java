package me.chancesd.sdutils.scheduler;

import org.bukkit.plugin.Plugin;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class WrappedFoliaTask implements SDTask {

	private final ScheduledTask task;

	public WrappedFoliaTask(final ScheduledTask task) {
		this.task = task;
	}

	@Override
	public void cancel() {
		task.cancel();
	}

	@Override
	public int getTaskID() {
		return -1;
	}

	@Override
	public boolean isCancelled() {
		return task.isCancelled();
	}

	@Override
	public Plugin getPlugin() {
		return task.getOwningPlugin();
	}

}
