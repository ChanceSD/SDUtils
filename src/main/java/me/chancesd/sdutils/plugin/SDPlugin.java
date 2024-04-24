package me.chancesd.sdutils.plugin;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class SDPlugin extends JavaPlugin {

	protected Listener registerListener(final Listener listener) {
		this.getServer().getPluginManager().registerEvents(listener, this);
		return listener;
	}

	public void reload() {
		onDisable();
		HandlerList.unregisterAll(this);
		Bukkit.getScheduler().cancelTasks(this);
		onEnable();
	}

}
