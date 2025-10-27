package me.chancesd.sdutils.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import me.chancesd.sdutils.command.BaseCommand;
import me.chancesd.sdutils.command.PluginHelpManager;
import me.chancesd.sdutils.display.DisplayManager;
import me.chancesd.sdutils.library.PluginLibraries;
import me.chancesd.sdutils.scheduler.ScheduleUtils;
import me.chancesd.sdutils.utils.Log;

public abstract class SDPlugin extends JavaPlugin {

	private DisplayManager displayManager;
	private boolean isReloading;
	private final Map<String, BaseCommand> registeredCommands = new HashMap<>();

	protected Listener registerListener(final Listener listener) {
		this.getServer().getPluginManager().registerEvents(listener, this);
		return listener;
	}

	/**
	 * Register a BaseCommand with the plugin and track it in the command registry.
	 *
	 * @param command  The PluginCommand from plugin.yml
	 * @param executor The BaseCommand executor
	 * @return The registered BaseCommand for method chaining
	 */
	protected BaseCommand registerCommand(final PluginCommand command, final BaseCommand executor) {
		if (command == null) {
			return executor;
		}

		command.setExecutor(executor);
		if (executor != null) {
			registeredCommands.put(command.getName(), executor);
		}
		return executor;
	}

	/**
	 * Register a BaseCommand using a factory function for cleaner one-liner registration.
	 *
	 * @param commandName The name of the command as defined in plugin.yml
	 * @param factory     Function that creates the BaseCommand given the PluginCommand
	 * @return The registered BaseCommand for method chaining
	 */
	protected BaseCommand registerCommand(final String commandName, final Function<PluginCommand, BaseCommand> factory) {
		final PluginCommand command = getCommand(commandName);
		if (command == null) {
			return null;
		}
		return registerCommand(command, factory.apply(command));
	}

	/**
	 * Get all registered BaseCommands.
	 *
	 * @return Collection of all registered BaseCommands
	 */
	public Collection<BaseCommand> getRegisteredCommands() {
		return new ArrayList<>(registeredCommands.values());
	}

	/**
	 * Get a registered BaseCommand by name.
	 *
	 * @param commandName The command name
	 * @return The BaseCommand or null if not found
	 */
	public BaseCommand getRegisteredCommand(final String commandName) {
		return registeredCommands.get(commandName.toLowerCase());
	}

	/**
	 * Get all command names that are registered as BaseCommands.
	 *
	 * @return List of command names
	 */
	public List<String> getRegisteredCommandNames() {
		return new ArrayList<>(registeredCommands.keySet());
	}

	/**
	 * Create a PluginHelpManager for generating plugin-wide help menus.
	 * This automatically discovers all registered BaseCommands and generates
	 * comprehensive help displays with proper formatting and permissions.
	 *
	 * @return A PluginHelpManager instance for this plugin
	 */
	public PluginHelpManager createHelpManager() {
		return new PluginHelpManager(this);
	}

	public abstract void onPluginLoad();

	public abstract void onPluginEnable();

	public abstract void onPluginDisable();

	@Override
	public void onLoad() {
		Log.setup(this);
		PluginLibraries.checkDependencies(this);
		onPluginLoad();
	}

	@Override
	public void onEnable() {
		ScheduleUtils.setupExecutor(this);
		displayManager = new DisplayManager();
		onPluginEnable();
		checkJavaVersion();
	}

	@Override
	public void onDisable() {
		onPluginDisable();
		displayManager.cleanup();
		ScheduleUtils.cancelAllTasks();
	}

	public void reload() {
		setReloading(true);
		onDisable();
		HandlerList.unregisterAll(this);
		Bukkit.getScheduler().cancelTasks(this);
		onEnable();
		setReloading(false);
	}

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
		if (javaVersion < 21) {
			Log.warning("You appear to be using Java 20 or lower. For now the plugin still works but please update to Java 21+");
			Log.warning("In the future this plugin will stop supporting Java versions this old");
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
