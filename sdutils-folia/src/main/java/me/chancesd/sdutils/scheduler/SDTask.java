package me.chancesd.sdutils.scheduler;

import org.bukkit.plugin.Plugin;

public interface SDTask {

	public void cancel();

	public boolean isCancelled();

	public Plugin getPlugin();

	public int getTaskID();

}
