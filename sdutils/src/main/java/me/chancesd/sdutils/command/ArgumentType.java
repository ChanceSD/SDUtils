package me.chancesd.sdutils.command;

/**
 * Enumeration of supported argument types for BaseCommand validation and tab completion.
 */
public enum ArgumentType {
	/** A simple string argument */
	STRING,

	/** A player name argument that must match an online player */
	PLAYER,

	/** An integer number argument */
	INTEGER,

	/** A double precision number argument */
	DOUBLE,

	/** A boolean argument (true/false, yes/no, on/off) */
	BOOLEAN,

	/** A player name argument that accepts "*" wildcard or online player names */
	PLAYER_OR_ALL,

	/** A string array that captures all remaining arguments as a single string */
	STRING_ARRAY,
	/** A world name argument that must match an existing world */
	WORLD,

	/** A material name argument that must match a valid Bukkit Material */
	MATERIAL,

	/** A duration argument (e.g., "30s", "5m", "2h", "1d") */
	DURATION
}
