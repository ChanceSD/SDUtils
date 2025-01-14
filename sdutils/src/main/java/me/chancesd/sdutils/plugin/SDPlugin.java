package me.chancesd.sdutils.plugin;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import me.chancesd.sdutils.display.DisplayManager;
import me.chancesd.sdutils.library.PluginLibraries;
import me.chancesd.sdutils.utils.Log;

public abstract class SDPlugin extends JavaPlugin {

	private DisplayManager displayManager;
	private boolean isReloading;

	protected Listener registerListener(final Listener listener) {
		this.getServer().getPluginManager().registerEvents(listener, this);
		return listener;
	}

	@Override
	public void onLoad() {
		PluginLibraries.checkDependencies(this);
		displayManager = new DisplayManager();
	}

	public void reload() {
		setReloading(true);
		onDisable();
		HandlerList.unregisterAll(this);
		Bukkit.getScheduler().cancelTasks(this);
		onEnable();
		setReloading(false);
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

	public DisplayManager getDisplayManager() {
		return displayManager;
	}

	public boolean isReloading() {
		return isReloading;
	}

	public void setReloading(final boolean isReloading) {
		this.isReloading = isReloading;
	}

}
