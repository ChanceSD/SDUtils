package me.chancesd.sdutils.scheduler;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class WrappedBukkitTask implements SDTask {

	private final BukkitTask task;

	public WrappedBukkitTask(final BukkitTask task) {
		this.task = task;
	}

	@Override
	public void cancel() {
		task.cancel();
	}

	@Override
	public int getTaskID() {
		return task.getTaskId();
	}

	@Override
	public boolean isCancelled() {
		return task.isCancelled();
	}

	@Override
	public Plugin getPlugin() {
		return task.getOwner();
	}

}
