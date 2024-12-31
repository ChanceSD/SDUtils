package me.chancesd.sdutils.library;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.yaml.snakeyaml.reader.UnicodeReader;

import me.chancesd.sdutils.utils.Log;
import me.chancesd.sdutils.utils.Utils;

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

	public static void loadDependency(final Plugin plugin, final String group, final String artifact, final String version, final String pattern,
			final String relocated) {
		MavenCentralDependency dependency;
		if (pattern.isEmpty())
			dependency = new MavenCentralDependency(plugin, group, artifact, version);
		else
			dependency = new MavenCentralDependency(plugin, group, artifact, version).withRelocation(pattern, relocated);

		dependency.load(name -> Log.info("Library " + name + " loaded!"), ex -> {
			Log.info(ex.getMessage());
			ex.printStackTrace();
		});
	}

	public static void loadDependency(final Plugin plugin, final String group, final String artifact, final String version) {
		loadDependency(plugin, group, artifact, version, "", "");
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
			if (!Utils.isVersionAtLeast(Utils.stripTags(Bukkit.getVersion()), "1.21")) {
				try {
					// Mohist has getLibraries but it doesn't work properly
					Class.forName("com.mohistmc.MohistMCStart");
					return false;
				} catch (final ClassNotFoundException e) {
					// Not Mohist
				}
			}
			return true;
		} catch (final Exception ex) {
			// Server too old to support getLibraries.
			return false;
		}
	}
}
