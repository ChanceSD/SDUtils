package me.chancesd.sdutils.updater;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import org.bukkit.plugin.Plugin;

import me.chancesd.sdutils.utils.Log;

public class SpigotUpdater extends Updater {

	public SpigotUpdater(final Plugin plugin, final int id, final UpdateType type) {
		super(plugin, id, type, plugin.getName());
	}

	@Override
	protected boolean read() {
		try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.getId()).openStream();
		        Scanner scanner = new Scanner(inputStream)) {
			if (scanner.hasNext()) {
				this.versionName = scanner.next();
			}
		} catch (final IOException e) {
			Log.warning("Couldn't check for spigot updates, check your internet connection");
			this.setResult(UpdateResult.FAIL_DBO);
			return false;
		}
		return true;
	}

	@Override
	public final boolean downloadFile() {
		return false;
	}

	@Override
	public String getUpdateLink() {
		return "https://www.spigotmc.org/resources/" + getPlugin().getName().toLowerCase() + "." + getId();
	}

}
