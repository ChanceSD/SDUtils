package me.chancesd.sdutils.utils;

import org.bukkit.Bukkit;

/**
 * Utility class to get and compare MC versions efficiently
 *
 * @author ChanceSD
 */
public enum MCVersion {
	V1_20_4("1.20.4"),
	V1_20("1.20"),
	V1_19("1.19"),
	V1_18("1.18"),
	V1_17("1.17"),
	V1_16_5("1.16.5"),
	V1_16("1.16"),
	V1_15("1.15"),
	V1_14("1.14"),
	V1_13_1("1.13.1"),
	V1_13("1.13"),
	V1_12("1.12"),
	V1_11_2("1.11.2"),
	V1_11("1.11"),
	V1_10("1.10"),
	V1_9("1.9"),
	V1_8("1.8"),
	OLD("old");

	private static final MCVersion minecraftVersion = getMCVersion(
			Bukkit.getBukkitVersion().isEmpty() ? "0" : Utils.stripTags(Bukkit.getBukkitVersion()));
	private final String version;

	private MCVersion(final String version) {
		this.version = version;
	}

	public static MCVersion getMCVersion(final String version) {
		for (final MCVersion mcVersion : values()) {
			if (Utils.isVersionAtLeast(version, mcVersion.version))
				return mcVersion;
		}
		return MCVersion.OLD;
	}

	public static final boolean isAtLeast(final MCVersion version) {
		return minecraftVersion.ordinal() < version.ordinal();
	}

	public static final boolean isLowerThan(final MCVersion version) {
		return minecraftVersion.ordinal() >= version.ordinal();
	}

	public static MCVersion getMinecraftversion() {
		return minecraftVersion;
	}

}
