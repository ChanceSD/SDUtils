package me.chancesd.sdutils.library;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.yaml.snakeyaml.reader.UnicodeReader;

import me.chancesd.sdutils.utils.Log;

/**
 * Credit to ElgarL for some of these classes
 * https://github.com/ElgarL/GroupManager/blob/master/src/org/anjocaido/groupmanager/dependencies/DependencyManager.java
 *
 * @author ElgarL
 */
public class PluginLibraries {

	private PluginLibraries() {
	}

	/**
	 * If older than Spigot 1.16.5, download
	 * and inject to the ClassPath all libraries
	 * as specified in the plugin.yml.
	 *
	 * @param plugin the Plugin instance
	 * @return true if all dependencies are OK.
	 */
	public static boolean checkDependencies(final Plugin plugin) {
		if (hasLibraries())
			return true;

		final AtomicBoolean isLoaded = new AtomicBoolean(true);

		final Consumer<Exception> error = ex -> { // This consumer runs if an error occurs while loading the dependency
			Log.info(ex.getMessage());
			ex.printStackTrace();
			isLoaded.set(false);
		};
		// This consumer runs on a successful load.
		final Consumer<String> loaded = name -> Log.info("Library " + name + " loaded!");

		// Read all libraries from the plugin.yml and manually load them (pre 1.16.5).
		final YamlConfiguration description = YamlConfiguration.loadConfiguration(new UnicodeReader(plugin.getResource("plugin.yml")));
		final List<?> libraries = description.getList("libraries");

		if (libraries != null) {
			libraries.forEach(lib -> {
				final String[] split = lib.toString().split(":");
				if (split.length == 3) {
					new MavenCentralDependency(plugin, split[0], split[1], split[2]).load(loaded, error);
				}
			});
		}

		return isLoaded.get();
	}

	/**
	 * Does Spigot support library loading (1.16.5+)
	 *
	 * @return true if supported.
	 */
	public static boolean hasLibraries() {
		try {
			// Doesn't exist before 1.16.5
			PluginDescriptionFile.class.getMethod("getLibraries");
			return true;
		} catch (final Exception ex) {
			// Server too old to support getLibraries.
			return false;
		}
	}
}
