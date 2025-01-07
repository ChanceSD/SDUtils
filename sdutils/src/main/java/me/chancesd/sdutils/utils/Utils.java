package me.chancesd.sdutils.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class Utils {

	private static boolean isPaper = hasClass("com.destroystokyo.paper.PaperConfig") || hasClass("io.papermc.paper.configuration.Configuration");

	private Utils() {
	}

	private static boolean hasClass(final String className) {
		try {
			Class.forName(className);
			return true;
		} catch (final ClassNotFoundException e) {
			return false;
		}
	}

	public static <T extends Event> void registerEvent(final Plugin plugin, final Listener listener, final Class<T> eventClass, final Consumer<T> handler) {
		Bukkit.getPluginManager().registerEvent(eventClass, listener, EventPriority.NORMAL, (l, event) -> {
			if (eventClass.isInstance(event)) {
				handler.accept(eventClass.cast(event));
			}
		}, plugin, true);
	}

	public static boolean isPaper() {
		return isPaper;
	}

	public static final boolean isVersionAtLeast(final String v1, final String v2) {
		if (v1.equals(v2))
			return true;

		final String[] v1Array = v1.split("\\.");
		final String[] v2Array = v2.split("\\.");
		final int length = Math.max(v2Array.length, v1Array.length);
		try {
			for (int i = 0; i < length; i++) {
				final int x = i < v2Array.length ? Integer.parseInt(v2Array[i]) : 0;
				final int y = i < v1Array.length ? Integer.parseInt(v1Array[i]) : 0;
				if (y > x)
					return true;
				if (y < x)
					return false;
			}
		} catch (final NumberFormatException ex) {
			Log.severe("Error reading version number! Comparing " + v1 + " to " + v2);
		}
		return true;
	}

	public static void renameFile(final File file, final String name) {
		try {
			Files.move(file.toPath(), file.toPath().resolveSibling(name), StandardCopyOption.REPLACE_EXISTING);
		} catch (final IOException e) {
			Log.severe("Error resetting config file", e);
		}
	}

	public static String stripTags(final String version) {
		return version.replaceAll("[-;+].+", "");
	}

	public static double roundTo1Decimal(final double value) {
		return Math.round(value * 10.0) / 10.0;
	}

	public static double roundTo2Decimal(final double value) {
		return Math.round(value * 100.0) / 100.0;
	}

}
