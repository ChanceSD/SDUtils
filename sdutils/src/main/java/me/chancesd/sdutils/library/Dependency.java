package me.chancesd.sdutils.library;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;

import org.bukkit.plugin.Plugin;
import org.xml.sax.SAXException;

/**
 * Single Thread dependency management.
 *
 * Thanks to https://www.spigotmc.org/resources/dependencymanager.62275/
 * DotRar for the original idea.
 *
 */
public abstract class Dependency {

	protected final Plugin plugin;

	Dependency(final Plugin plugin) {
		this.plugin = plugin;
	}

	protected abstract URL buildUrl() throws IOException, ParserConfigurationException, SAXException;

	void load(final Consumer<String> onComplete, final Consumer<Exception> onError) {
		try {
			final File cacheFolder = new File(plugin.getDataFolder(), "libraries");
			if (!cacheFolder.exists())
				cacheFolder.mkdir();
			final Consumer<File> inject = f -> {
				try {
					InjectorUtils.INSTANCE.loadJar(plugin, f);
					onComplete.accept(f.getName());
				} catch (final Exception ex) {
					onError.accept(ex);
				}
			};

			final File cached = new File(cacheFolder, getLocalName());
			if (cached.length() == 0) {
				cached.delete();
			}

			if (!cached.exists() && cached.createNewFile()) {
				download(cached, inject, onError);
			} else {
				inject.accept(cached);
			}

		} catch (final Exception ex) {
			onError.accept(ex);
		}
	}

	protected abstract String getLocalName();

	private void download(final File downloadDest, final Consumer<File> inject, final Consumer<Exception> onError) {
		try {
			final URL url = buildUrl();
			final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.44");

			final InputStream stream = conn.getInputStream();
			final ReadableByteChannel byteChan = Channels.newChannel(stream);

			final FileOutputStream fos = new FileOutputStream(downloadDest);
			final FileChannel fileChan = fos.getChannel();

			fileChan.transferFrom(byteChan, 0, Long.MAX_VALUE);

			fos.close();
			stream.close();

			inject.accept(downloadDest);

		} catch (IOException | ParserConfigurationException | SAXException ex) {
			onError.accept(ex);
		}
	}
}
