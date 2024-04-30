package me.chancesd.sdutils.plugin;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import me.chancesd.sdutils.utils.Log;

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

	@SuppressWarnings("unused")
	private void checkJavaVersion() {
		int javaVersion;
		String version = System.getProperty("java.version");
		if (version.startsWith("1.")) {
			version = version.substring(2, 3);
		} else {
			final int dot = version.indexOf(".");
			if (dot != -1) {
				version = version.substring(0, dot);
			} else {
				final int separator = version.indexOf("-");
				if (separator != -1) {
					version = version.substring(0, separator);
				}
			}
		}
		try {
			javaVersion = Integer.parseInt(version);
		} catch (final NumberFormatException e) {
			return;
		}
		if (javaVersion < 17) {
			Log.severe("You appear to be using Java 16 or lower. For now the plugin still works but please update to Java 17+");
			Log.severe("In the future this plugin will stop supporting Java versions this old");
		}
	}

}
