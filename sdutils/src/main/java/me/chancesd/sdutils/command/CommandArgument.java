package me.chancesd.sdutils.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import me.chancesd.sdutils.utils.NCDuration;

/**
 * Represents a parsed command argument with type-safe conversion methods.
 * This class provides convenient methods to convert string arguments to their expected types
 * with proper validation and error handling.
 */
public class CommandArgument {
	private final String value;

	public CommandArgument(final String value) {
		this.value = value;
	}

	/**
	 * Gets the raw string value of this argument.
	 * 
	 * @return the raw argument value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Converts this argument to an integer.
	 * 
	 * @return the integer value
	 * @throws IllegalStateException if the value is not a valid integer
	 */
	public int getAsInt() {
		try {
			return Integer.parseInt(value);
		} catch (final NumberFormatException e) {
			throw new IllegalStateException("Not a valid integer: " + value);
		}
	}

	/**
	 * Converts this argument to a double.
	 * 
	 * @return the double value
	 * @throws IllegalStateException if the value is not a valid number
	 */
	public double getAsDouble() {
		try {
			return Double.parseDouble(value);
		} catch (final NumberFormatException e) {
			throw new IllegalStateException("Not a valid number: " + value);
		}
	}

	/**
	 * Converts this argument to a boolean.
	 * Accepts: true/false, yes/no, on/off (case insensitive)
	 * 
	 * @return the boolean value
	 * @throws IllegalStateException if the value is not a valid boolean
	 */
	public boolean getAsBoolean() {
		if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
			return true;
		}
		if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
			return false;
		}
		throw new IllegalStateException("Not a valid boolean: " + value);
	}

	/**
	 * Checks if this argument is a wildcard "*".
	 * 
	 * @return true if the value is "*"
	 */
	public boolean isWildcard() {
		return "*".equals(value);
	}

	/**
	 * Alias for {@link #isWildcard()} for better readability in player contexts.
	 * 
	 * @return true if the value represents "all players"
	 */
	public boolean isAllPlayers() {
		return isWildcard();
	}

	/**
	 * Converts this argument to a Player object, or null if it's a wildcard.
	 * 
	 * @return the Player object, or null if wildcard
	 */
	public Player getAsPlayerOrNull() {
		return isWildcard() ? null : Bukkit.getPlayer(value);
	}
	/**
	 * Converts this argument to a World object.
	 * 
	 * @return the World object
	 * @throws IllegalStateException if the world is not found
	 */
	public World getAsWorld() {
		final World world = Bukkit.getWorld(value);
		if (world == null) {
			throw new IllegalStateException("World not found: " + value);
		}
		return world;
	}

	/**
	 * Converts this argument to a Material object.
	 * 
	 * @return the Material object
	 * @throws IllegalStateException if the material is not valid
	 */
	public Material getAsMaterial() {
		try {
			return Material.valueOf(value.toUpperCase());
		} catch (final IllegalArgumentException e) {
			throw new IllegalStateException("Invalid material: " + value);
		}
	}
	/**
	 * Converts this argument to an NCDuration object.
	 * Supports formats like "30s", "5m", "2h", "1d", or combinations like "1h30m"
	 * 
	 * @return the NCDuration object
	 * @throws IllegalStateException if the value is not a valid duration format
	 */
	public NCDuration getAsDuration() {
		try {
			return NCDuration.parseDuration(value);
		} catch (final IllegalArgumentException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}
