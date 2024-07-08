package me.chancesd.sdutils.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Utils {

	private Utils() {
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
		return Math.round(value * 100.0) / 100.0;
	}

}
