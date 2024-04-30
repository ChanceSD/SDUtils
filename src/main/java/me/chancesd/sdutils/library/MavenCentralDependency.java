package me.chancesd.sdutils.library;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

import org.bukkit.plugin.Plugin;
import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import com.alessiodp.libby.Library.Builder;
import com.alessiodp.libby.relocation.Relocation;

/**
 * Single Thread dependency management.
 *
 * Thanks to https://www.spigotmc.org/resources/dependencymanager.62275/
 * DotRar for the original idea.
 */
public class MavenCentralDependency extends Dependency {

	private static final String ROOT_URL = "https://repo1.maven.org/maven2";
	private final String group;
	private final String artifact;
	private final String version;
	private Relocation relocation;

	/**
	 * Single Threaded dependency management.
	 * Forces all dependencies to be available before loading.
	 *
	 * @param plugin   calling Plugin
	 * @param group    groupId
	 * @param artifact artifactId
	 * @param version  dependency version
	 */
	MavenCentralDependency(final Plugin plugin, final String group, final String artifact, final String version) {
		super(plugin);

		this.group = group;
		this.artifact = artifact;
		this.version = version;
	}

	MavenCentralDependency withRelocation(final String pattern, final String relocatedPattern) {
		this.relocation = new Relocation(pattern, relocatedPattern);
		return this;
	}

	@Override
	void load(final Consumer<String> onComplete, final Consumer<Exception> onError) {
			try {
				final BukkitLibraryManager libraryManager = new BukkitLibraryManager(plugin, "libraries");
				libraryManager.addMavenCentral();
				final Builder libBuilder = Library.builder().groupId(group).artifactId(artifact).version(version);
				if (relocation != null)
					libBuilder.relocate(relocation);
				libraryManager.loadLibrary(libBuilder.build());
				onComplete.accept(artifact);
			} catch (final Exception ex) {
				onError.accept(ex);
			}
	}

	@Override
	protected URL buildUrl() throws MalformedURLException {
		final String groupSlashed = String.join("/", group.split("\\."));
		final String jarName = String.format("%s-%s.jar", artifact, version);
		return new URL(String.format("%s/%s/%s/%s/%s", ROOT_URL, groupSlashed, artifact, version, jarName));
	}

	@Override
	protected String getLocalName() {
		return String.format("%s-%s.jar", artifact, version);
	}
}
